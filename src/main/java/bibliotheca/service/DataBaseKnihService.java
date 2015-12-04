package bibliotheca.service;

import bibliotheca.model.VOFileDetail;
import org.jsoup.nodes.Document;

import java.io.File;
import java.util.Map;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 4.12.15
 */
public interface DataBaseKnihService {

    int CONNECT_TIMEOUT_MILLIS = 10000; // timeout for connect to external source

    // TODO - JavaDoc - Lebeda
    boolean loadFromDBKnih(Map<String, Object> metadata, VOFileDetail fileDetail, String url);

    @Deprecated // make it private
    // TODO - JavaDoc - Lebeda
    public Document getDocument(String url);

    // TODO - JavaDoc - Lebeda
    String getAutomaticDBKnihUrl(String bookname);

    // TODO - JavaDoc - Lebeda
    void tryDb(String path, String tryDB, File file);

    // TODO - JavaDoc - Lebeda
    String getDBKnihDescription(String url);

    // TODO - JavaDoc - Lebeda
    String getHodnoceniDbPocet(String dbknihUrl);

    // TODO - JavaDoc - Lebeda
    String getHodnoceniDbProcento(String dbknihUrl);
}
