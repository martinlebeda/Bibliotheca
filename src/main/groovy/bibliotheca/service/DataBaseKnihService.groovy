package bibliotheca.service

import bibliotheca.model.*
import bibliotheca.tools.Tools
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.CompareToBuilder
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import javax.validation.constraints.NotNull
import java.nio.file.Paths
import java.util.stream.Collectors

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 4.12.15
 */
@Service
public class DataBaseKnihService {

    static int CONNECT_TIMEOUT_MILLIS = 10000; // timeout for connect to external source

    @Autowired
    private BrowsePageService browsePageService;

    @Autowired
    private FtMetaService ftMetaService;

    @Autowired
    private BookDetailService bookDetailService;

    private Map<String, Document> cacheDoc = new HashMap<>();

    /**
     * fill all available information from databazeknih.cz to fileDetail and save them to disk
     *
     * @param fileDetail detail of book
     * @param force force reload metadata
     */
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
            bookDetailService.writeMetaData(fileDetail.getPath(), fileDetail.getBookFileName(), fileDetail.getMetadata());
        }
    }

    /**
     * Download cover from databaze knih
     *
     * @param fileDetail filled detail with url
     * @param force force redownload if cover exists
     */
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

    // TODO - JavaDoc - Lebeda
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

        ftMetaService.put(bookDetailService.getVoFileDetail(path, key));
    }

    /**
     * Clear metadata of book.
     * Its delete cover, yaml metadata and description.
     *
     * @param path path to store directory
     * @param key name of book file without suffix
     */
    public List<VOFileDetail> getChooseJoinModalDataList(String path, String name) {
        // najít všechny knihy v directory
        File[] files = new File(path).listFiles();
        List<VOPath> voPathList = Arrays.asList(files).stream()
                .filter({ file -> !StringUtils.startsWith(file.getName(), name) })
                .filter({ it.isFile() })
                .map({ new VOPath(it) })
                .collect(Collectors.toList());

        return browsePageService.getVoFileDetails(voPathList).stream()
                .sorted({ o1, o2 ->
            new CompareToBuilder()
                    .append(o2.getDbknihUrlExists(), o1.getDbknihUrlExists())
                    .append(o1.getTitle(), o2.getTitle())
                    .toComparison()
        } as Comparator<? super VOFileDetail>)
                .collect(Collectors.toList());
    }

    // TODO - JavaDoc - Lebeda
    public List<VODirData> getChooseJoinModalDirDataList(String path) {
        File file = new File(path);
        File[] files = file.getParentFile().listFiles();
        assert files != null;
        return Arrays.asList(files).stream()
                .filter({ it.isDirectory() })
                .filter({ file1 -> !StringUtils.equals(file1.getName(), file.getName()) })
                .map({ new VODirData(it) })
                .sorted({ o1, o2 ->
            new CompareToBuilder()
                    .append(o2.getTitle(), o1.getTitle())
                    .toComparison()
        } as Comparator<? super VODirData>)
                .collect(Collectors.toList());
    }

    /**
     * fill all available information from databazeknih.cz to fileDetail and save them to disk
     *
     * @param fileDetail detail of book
     */
    public void loadFromDBKnih(VOFileDetail fileDetail) {
        loadFromDBKnih(fileDetail, false);
    }

/**
 * Get raw loaded data from databazeknih.cz.
 * Iternally use cache.
 *
 * @param url url to databazeknih.cz
 * @return Loaded data from site databazeknih.cz
 *
 * TODO Lebeda - will be change to private
 */
    public Document getDocument(String url) {
        if (cacheDoc.containsKey(url)) {
            return cacheDoc.get(url);
        } else {
            Document doc = null
            try {
                doc = Jsoup.connect(url).timeout(CONNECT_TIMEOUT_MILLIS).get();
            cacheDoc.put(url, doc);
            } catch (UnknownHostException e) {
                // ignorovat
            }
            return doc;
        }
    }

    /**
     * tray find book in databazeknih.cz and automatically fill extracted metadata.
     *
     * @param fileDetail detail of book
     */
    public boolean tryDb(VOFileDetail fileDetail) {
        String dbKnihUrl = getAutomaticDBKnihUrl(fileDetail.getBookname());
        if (StringUtils.isNotBlank(dbKnihUrl)) {
            fileDetail.setDbknihUrl(dbKnihUrl);
            String description = getDBKnihDescription(dbKnihUrl);
            loadFromDBKnih(fileDetail);

            bookDetailService.writeMetaData(fileDetail.getPath(), fileDetail.getBookFileName(), fileDetail.getMetadata());

            if (StringUtils.isNotBlank(description) && StringUtils.isBlank(fileDetail.getDesc())) {
                Tools.writeDescription(fileDetail.getPath(), fileDetail.getBookFileName(), description);
                fileDetail.setDesc(description);
            }

            ftMetaService.put(fileDetail);

            return true;
        } else {
            return false;
        }
    }

    /**
     * Extract book description from databazeknih.cz
     *
     * @param url url to databazeknih.cz
     * @return book desctiotion
     */
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

    /**
     * Extract count of ratings from databazeknih.cz.
     *
     * @param url url to databazeknih.cz
     * @return count of ratings
     */
    public String getHodnoceniDbPocet(String url) {
        String result = "";
        Elements elements;
        elements = getDocument(url).select("#voixis > div > a.bpointsm");
        for (Element element : elements) {
            result = element.text().replaceAll(" hodnocení", "");
        }
        return result;
    }

/**
 * Extract average rating from databazeknih.cz.
 *
 * @param url url to databazeknih.cz
 * @return average rating
 */
    public String getHodnoceniDbProcento(String url) {
        String result = "";
        Elements elements;
        elements = getDocument(url).select("#voixis > div > a.bpoints");
        for (Element element : elements) {
            result = element.text().replaceAll("%", "");
        }
        return result;
    }

    /**
     * Extract metadata from databazeknih.cz.
     * Url maybe in local cache.
     *
     * @param url url to book page in databazeknih.cz
     * @return metadata
     */
    public String getTitle(String url) {
        String result = "";
        Elements elements;
        elements = getDocument(url).select("h1[itemprop=name]");
        for (Element element : elements) {
            result = element.text();
        }
        return result;
    }

    /**
     * Extract metadata from databazeknih.cz.
     * Url maybe in local cache.
     *
     * @param url url to book page in databazeknih.cz
     * @return metadata
     */
    public String getSerie(String url) {
        String result = "";
        Elements elements;
        elements = getDocument(url).select("a[href^=serie]");
        for (Element element : elements) {
            result = element.parent().text().replaceAll("[().]", "");
        }
        return result;
    }

    /**
     * Extract metadata from databazeknih.cz.
     * Url maybe in local cache.
     *
     * @param url url to book page in databazeknih.cz
     * @return metadata
     */
    public List<String> getAuthors(String url) {
        List<String> result = new ArrayList<>();
        Elements elements;
        //                                      #left_less > div:nth-child(4) > h2 > a:nth-child(2)
        elements = getDocument(url).select("h2.jmenaautoru > a[href^=autori]");
        result.addAll(elements.stream().map({it.text()}).collect(Collectors.toList()));
        return result;
    }

    /**
     * Choose list for founded books.
     *
     * @param bookname book name for find
     * @return list for founded books
     */
    public List<VOChoose> getChooseDbModalList(String bookname) {
        Document doc = Jsoup.connect("http://www.databazeknih.cz/search?q=" + URLEncoder.encode(bookname) + "&hledat=&stranka=search").timeout(CONNECT_TIMEOUT_MILLIS).get();

        Elements elements = doc.select("#left_less > p.new_search");
//            elements = doc.select("#left_less > p.new_search > a.search_to_stats.strong");
        return elements.stream()
                .map({ element ->
            new VOChoose(
                    element.select("img").attr("src"),
                    "http://www.databazeknih.cz/" + element.select("a.search_to_stats").attr("href"),
                    element.select("a.search_to_stats").text(),
                    element.select(".smallfind").text())
        })
                .sorted({ o1, o2 ->
            new CompareToBuilder()
                    .append(o1.getAuthorSurrname(), o2.getAuthorSurrname())
                    .append(o1.getAuthorFirstname(), o2.getAuthorFirstname())
                    .append(o1.getTitle(), o2.getTitle())
                    .toComparison()
        } as Comparator)
                .collect(Collectors.toList());
    }

    /**
     * Get automaticly url to databazeknih.cz by book name.
     * If search return only one record, return it.
     *
     * @param bookname name of book (usually last part of filename without suffix)
     * @return url for databazeknih.cz
     */
    public String getAutomaticDBKnihUrl(String bookname) {
        String dbknih = "";
        Document doc = Jsoup.connect("http://www.databazeknih.cz/search?q=" + URLEncoder.encode(bookname) + "&hledat=&stranka=search").timeout(DataBaseKnihService.CONNECT_TIMEOUT_MILLIS).get();

        Elements elements;
        elements = doc.select("#left_less > p.new_search > a.search_to_stats.strong");

        if (elements.size() == 1) {
            for (Element element : elements) {
                dbknih = "http://www.databazeknih.cz/" + element.attr("href");
            }
        }

        return dbknih;
    }
}
