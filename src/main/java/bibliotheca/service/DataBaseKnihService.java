package bibliotheca.service;

import bibliotheca.model.VOChoose;
import bibliotheca.model.VOFileDetail;
import org.jsoup.nodes.Document;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Rutines for loading and extract data from site databazeknih.cz.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 4.12.15
 */
public interface DataBaseKnihService {

    int CONNECT_TIMEOUT_MILLIS = 10000; // timeout for connect to external source

    /**
     * fill all available information from databazeknih.cz to fileDetail and save them to disk
     *
     * @param fileDetail detail of book
     */
    void loadFromDBKnih(VOFileDetail fileDetail);

    /**
     * fill all available information from databazeknih.cz to fileDetail and save them to disk
     *
     * @param fileDetail detail of book
     * @param force force reload metadata
     */
    void loadFromDBKnih(VOFileDetail fileDetail, boolean force);

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
     * @param fileDetail detail of book
     */
    void tryDb(VOFileDetail fileDetail);

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

    // TODO - JavaDoc - Lebeda
    String getNazev(String url);

    // TODO - JavaDoc - Lebeda
    String getSerie(String url);

    // TODO - JavaDoc - Lebeda
    List<String> getAuthors(String url);

    // TODO - JavaDoc - Lebeda
    List<VOChoose> getChooseDbModalList(String bookname);

    // TODO - JavaDoc - Lebeda
    void downloadCover(@NotNull VOFileDetail fileDetail, boolean force);

    // TODO - JavaDoc - Lebeda
    void clearMetadata(String path, String key);
}
