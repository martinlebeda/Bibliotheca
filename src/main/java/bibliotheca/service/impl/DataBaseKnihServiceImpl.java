package bibliotheca.service.impl;

import bibliotheca.model.VOFileDetail;
import bibliotheca.service.DataBaseKnihService;
import bibliotheca.tools.Tools;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 4.12.15
 */
@Service
public class DataBaseKnihServiceImpl implements DataBaseKnihService {

    private Map<String, Document> cacheDoc = new HashMap<>();

    @Override
    public boolean loadFromDBKnih(Map<String, Object> metadata, VOFileDetail fileDetail, String url) {
        boolean saveChange = false;
        Document doc = getDocument(url);

        String nazev = getDBKnihNazev(doc);
        if (StringUtils.isNotBlank(nazev)) {
            if (fileDetail != null) {
                fileDetail.setNazev(nazev);
            }
            metadata.put(Tools.METADATA_KEY_NAZEV, nazev);
            saveChange = true;
        }

        String serie = getDBKnihSerie(doc);
        if (StringUtils.isNotBlank(serie)) {
            if (fileDetail != null) {
                fileDetail.setSerie(serie);
            }
            metadata.put(Tools.METADATA_KEY_SERIE, serie);
            saveChange = true;
        }

        List<String> authors = getDBKnihAuthors(doc);
        if (CollectionUtils.isNotEmpty(authors)) {
            if (fileDetail != null) {
                fileDetail.getAuthors().clear();
                fileDetail.getAuthors().addAll(authors);
            }
            metadata.put(Tools.METADATA_KEY_AUTHORS, authors);
            saveChange = true;
        }
        return saveChange;
    }

    @Override
    public Document getDocument(String url) {
        if (cacheDoc.containsKey(url)) {
            return cacheDoc.get(url);
        } else {
            try {
                Document doc = Jsoup.connect(url).timeout(CONNECT_TIMEOUT_MILLIS).get();
                cacheDoc.put(url, doc);
                return doc;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public void tryDb(String path, String tryDB, File file) {
        String bookname = StringUtils.replacePattern(tryDB, ".*- *", "");
        Map<String, Object> metadata = Tools.getStringStringMap(path, tryDB);
        String dbKnihUrl = getAutomaticDBKnihUrl(bookname);
        if (StringUtils.isNotBlank(dbKnihUrl)) {
            metadata.put(Tools.METADATA_KEY_DATABAZEKNIH_CZ, dbKnihUrl);

            String description = getDBKnihDescription(dbKnihUrl);
            loadFromDBKnih(metadata, null, dbKnihUrl);

            Tools.writeMetaData(path, tryDB, metadata);

            if (StringUtils.isNotBlank(description)) {
                String baseFileName = Paths.get(file.getAbsolutePath(), tryDB).toString();
                Tools.createDescription(baseFileName, description);
            }
        }
    }

    public String getDBKnihDescription(String url) {
        Document doc = getDocument(url);
        String frmDescription = "";
        Elements elements;
        elements = doc.select("#biall");
        for (Element element : elements) {
            frmDescription = element.text().replace("méně textu", "");
        }

        if (StringUtils.isBlank(frmDescription)) {
            elements = doc.select("#bdetail_rest > p");
            for (Element element : elements) {
                frmDescription = element.text().replace("méně textu", "");
            }
        }

        if (StringUtils.isBlank(frmDescription)) {
            elements = doc.select("#bdetail_rest_mid > p");
            for (Element element : elements) {
                frmDescription = element.text().replace("méně textu", "");
            }
        }

        return frmDescription;
    }

    @Override
    public String getHodnoceniDbPocet(String url) {
        String result = "";
        Elements elements;
        elements = getDocument(url).select("#voixis > div > a.bpointsm");
        for (Element element : elements) {
            result = element.text().replaceAll(" hodnocení", "");
        }
        return result;
    }

    @Override
    public String getHodnoceniDbProcento(String url) {
        String result = "";
        Elements elements;
        elements = getDocument(url).select("#voixis > div > a.bpoints");
        for (Element element : elements) {
            result = element.text().replaceAll("%", "");
        }
        return result;
    }

    private static String getDBKnihNazev(Document doc) {
        String result = "";
        Elements elements;
        elements = doc.select("h1[itemprop=name]");
        for (Element element : elements) {
            result = element.text();
        }
        return result;
    }

    private static String getDBKnihSerie(Document doc) {
        String result = "";
        Elements elements;
        elements = doc.select("a[href^=serie]");
        for (Element element : elements) {
            result = element.parent().text().replaceAll("[().]", "");
        }
        return result;
    }

    private static List<String> getDBKnihAuthors(Document doc) {
        List<String> result = new ArrayList<>();
        Elements elements;
        //                                      #left_less > div:nth-child(4) > h2 > a:nth-child(2)
        elements = doc.select("h2.jmenaautoru > a[href^=autori]");
        result.addAll(elements.stream().map(Element::text).collect(Collectors.toList()));
        return result;
    }

//    public static String getDBKnihNazev(Document doc) {
//
//    }

    @Override
    public String getAutomaticDBKnihUrl(String bookname) {
        String dbknih = "";
        try {

            Document doc = Jsoup.connect("http://www.databazeknih.cz/search?q=" + URLEncoder.encode(bookname) + "&hledat=&stranka=search").timeout(DataBaseKnihService.CONNECT_TIMEOUT_MILLIS).get();

            Elements elements;
            elements = doc.select("#left_less > p.new_search > a.search_to_stats.strong");

            if (elements.size() == 1) {
                for (Element element : elements) {
                    dbknih = "http://www.databazeknih.cz/" + element.attr("href");
                }
            }

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return dbknih;
    }
}
