package bibliotheca.service.impl;

import bibliotheca.model.*;
import bibliotheca.service.BrowsePageService;
import bibliotheca.service.DataBaseKnihService;
import bibliotheca.tools.Tools;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 4.12.15
 */
@Service
public class DataBaseKnihServiceImpl implements DataBaseKnihService {

    @Autowired
    private BrowsePageService browsePageService;

    private Map<String, Document> cacheDoc = new HashMap<>();

    @Override
    public void loadFromDBKnih(@NotNull VOFileDetail fileDetail, boolean force) {
        fileDetail.setTitle(getTitle(fileDetail.getDbknihUrl()));
        fileDetail.setSerie(getSerie(fileDetail.getDbknihUrl()));
        fileDetail.replaceAuthors(getAuthors(fileDetail.getDbknihUrl()));
        fileDetail.setHodnoceniDbPocet(getHodnoceniDbPocet(fileDetail.getDbknihUrl()));
        fileDetail.setHodnoceniDbProcento(getHodnoceniDbProcento(fileDetail.getDbknihUrl()));

        // automatic load cover if missing
        downloadCover(fileDetail, force);

        String description = getDBKnihDescription(fileDetail.getDbknihUrl());
        if ((StringUtils.isNotBlank(description) && StringUtils.isBlank(fileDetail.getDesc())) || force) {
            Tools.writeDescription(fileDetail.getPath(), fileDetail.getBookFileName(), description);
            fileDetail.setDesc(description);
        }

        if (fileDetail.isDirty()) {
            Tools.writeMetaData(fileDetail.getPath(), fileDetail.getBookFileName(), fileDetail.getMetadata());
        }
    }

    @Override
    public void downloadCover(@NotNull VOFileDetail fileDetail, boolean force) {
        if (StringUtils.isBlank(fileDetail.getCover()) || force) {
            Elements elements;
            String frmCover = null;

            elements = getDocument(fileDetail.getDbknihUrl()).select("img.kniha_img"); // obal knihy
            for (Element element : elements) {
                frmCover = element.attr("src");
            }

            if (StringUtils.isNoneBlank(frmCover)) {
                String baseFileName = Paths.get(new File(fileDetail.getPath()).getAbsolutePath(), fileDetail.getBookFileName()).toString();
                VOFile coverDb = Tools.downloadCover(baseFileName, frmCover);
                fileDetail.setCover(coverDb.getPath());
            }
        }
    }

    @Override
    public void clearMetadata(String path, String key) {
        File coverFile = Paths.get(path, key + ".jpg").toFile();
        if (coverFile.exists()) {
            coverFile.delete();
        }

        File noCoverFile = Paths.get(path, key + ".nocover").toFile();
        if (noCoverFile.exists()) {
            noCoverFile.delete();
        }

        File readmeFile = Paths.get(path, key + ".mkd").toFile();
        if (readmeFile.exists()) {
            readmeFile.delete();
        }

        File metadataFile = Paths.get(path, key + ".yaml").toFile();
        if (metadataFile.exists()) {
            metadataFile.delete();
        }
    }

    @Override
    public List<VOFileDetail> getChooseJoinModalDataList(String path, String name) {
        // najít všechny knihy v directory
        File[] files = new File(path).listFiles();
        List<VOPath> voPathList = Arrays.asList(files).stream()
                .filter(file -> !StringUtils.startsWith(file.getName(), name))
                .filter(File::isFile)
                .map(VOPath::new)
                .collect(Collectors.toList());

        return browsePageService.getVoFileDetails(voPathList);
    }

    @Override
    public void loadFromDBKnih(VOFileDetail fileDetail) {
       loadFromDBKnih(fileDetail, false);
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

    public void tryDb(VOFileDetail fileDetail) {
        String dbKnihUrl = getAutomaticDBKnihUrl(fileDetail.getBookname());
        if (StringUtils.isNotBlank(dbKnihUrl)) {
            fileDetail.setDbknihUrl(dbKnihUrl);
            String description = getDBKnihDescription(dbKnihUrl);
            loadFromDBKnih(fileDetail);

            Tools.writeMetaData(fileDetail.getPath(), fileDetail.getBookFileName(), fileDetail.getMetadata());

            if (StringUtils.isNotBlank(description) && StringUtils.isBlank(fileDetail.getDesc())) {
                Tools.writeDescription(fileDetail.getPath(), fileDetail.getBookFileName(), description);
                fileDetail.setDesc(description);
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

    @Override
    public String getTitle(String url) {
        String result = "";
        Elements elements;
        elements = getDocument(url).select("h1[itemprop=name]");
        for (Element element : elements) {
            result = element.text();
        }
        return result;
    }

    @Override
    public String getSerie(String url) {
        String result = "";
        Elements elements;
        elements = getDocument(url).select("a[href^=serie]");
        for (Element element : elements) {
            result = element.parent().text().replaceAll("[().]", "");
        }
        return result;
    }

    @Override
    public List<String> getAuthors(String url) {
        List<String> result = new ArrayList<>();
        Elements elements;
        //                                      #left_less > div:nth-child(4) > h2 > a:nth-child(2)
        elements = getDocument(url).select("h2.jmenaautoru > a[href^=autori]");
        result.addAll(elements.stream().map(Element::text).collect(Collectors.toList()));
        return result;
    }

    @Override
    public List<VOChoose> getChooseDbModalList(String bookname) {
        try {
            Document doc = Jsoup.connect("http://www.databazeknih.cz/search?q=" + URLEncoder.encode(bookname) + "&hledat=&stranka=search").timeout(CONNECT_TIMEOUT_MILLIS).get();

            Elements elements = doc.select("#left_less > p.new_search");
//            elements = doc.select("#left_less > p.new_search > a.search_to_stats.strong");
            return elements.stream()
                    .map(element -> new VOChoose(
                            element.select("img").attr("src"),
                            "http://www.databazeknih.cz/" + element.select("a.search_to_stats").attr("href"),
                            element.select("a.search_to_stats").text(),
                            element.select(".smallfind").text()))
                    .sorted((o1, o2) -> new CompareToBuilder()
                            .append(o1.getAuthorSurrname(), o2.getAuthorSurrname())
                            .append(o1.getAuthorFirstname(), o2.getAuthorFirstname())
                            .append(o1.getTitle(), o2.getTitle())
                            .toComparison())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

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
