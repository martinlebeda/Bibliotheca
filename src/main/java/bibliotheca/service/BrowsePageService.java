package bibliotheca.service;

import bibliotheca.model.VOFileDetail;
import bibliotheca.model.VOPath;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 4.12.15
 */
public interface BrowsePageService {

    /**
     * Get model for browsePage
     *
     * @return model for page render
     */
    Map<String, Object> getModel(String path, final String booksearch, final String basename);

    /**
     * Try automatic fetch metadata from databaze knih.
     *
     * @param path path to store directory
     * @param name name of book file without suffix
     * @return part of model for page render
     */
    // TODO Lebeda - přesun
    boolean tryDb(String path, String name);

    // TODO - JavaDoc - Lebeda
    List<VOFileDetail> getVoFileDetails(List<VOPath> fileList);

    /**
     * Load model for one book item.
     *
     * @param path path to store directory
     * @param name name of book file without suffix
     * @return part of model for page render
     */
    Map<String, Object> loadItemModel(String path, String name);

    // TODO - JavaDoc - Lebeda
    void tryDbAll(String path);

    // TODO - JavaDoc - Lebeda
    void tidyUpAll(String path);
}
