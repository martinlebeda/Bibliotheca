package bibliotheca.service;

import bibliotheca.model.VOFileDetail;
import org.jsoup.nodes.Document;

import java.io.File;
import java.util.Map;

/**
 * Rutines for loading and extract data from site databazeknih.cz.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 4.12.15
 */
public interface DataBaseKnihService {

    int CONNECT_TIMEOUT_MILLIS = 10000; // timeout for connect to external source

    /**
     * Load (not)all information from databazeknih.cz and fill to metadata and fileDetail
     *
     * @param metadata
     * @param fileDetail
     *
     * @param url url to databazeknih.cz
     * @return true if something changed
     * @deprecated will be break into more atomic operations
     */
    @Deprecated
    boolean loadFromDBKnih(Map<String, Object> metadata, VOFileDetail fileDetail, String url);

    /**
     * Get raw loaded data from databazeknih.cz.
     * Iternally use cache.
     *
     * @param url url to databazeknih.cz
     * @return Loaded data from site databazeknih.cz
     *
     * @deprecated will be change to private
     */
    @Deprecated
    public Document getDocument(String url);

    /**
     * Get automaticly url to databazeknih.cz by book name.
     * If search return only one record, return it.
     *
     * @param bookname name of book (usually last part of filename without suffix)
     * @return url for databazeknih.cz
     */
    String getAutomaticDBKnihUrl(String bookname);

    /**
     * tray find book in databazeknih.cz and automatically fill extracted metadata.
     *
     * @param path
     * @param tryDB
     * @param file
     *
     * @deprecated will be break to more atomic
     */
    @Deprecated
    void tryDb(String path, String tryDB, File file);

    /**
     * Extract book description from databazeknih.cz
     *
     * @param url url to databazeknih.cz
     * @return book desctiotion
     */
    String getDBKnihDescription(String url);

    /**
     * Extract count of ratings from databazeknih.cz.
     *
     * @param url url to databazeknih.cz
     * @return count of ratings
     */
    String getHodnoceniDbPocet(String url);

    /**
     * Extract average rating from databazeknih.cz.
     *
     * @param url url to databazeknih.cz
     * @return average rating
     */
    String getHodnoceniDbProcento(String url);
}
