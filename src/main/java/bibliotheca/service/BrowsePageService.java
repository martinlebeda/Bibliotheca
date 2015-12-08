package bibliotheca.service;

import java.util.Map;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 4.12.15
 */
public interface BrowsePageService {

    /**
     * Get model for browsePage
     *
     * @deprecated will be break to more atomic and interface will be dramatically changed
     */
    @Deprecated
    Map<String, Object> getModel(String path, final String booksearch, final String devicePath,
                                            final String target, final String tidyup, final String delete,
                                            final String basename, String tryDB);

    /**
     * Try automatic fetch metadata from databaze knih.
     *
     * @param path path to store directory
     * @param name name of book file without suffix
     * @return part of model for page render
     */
    // TODO Lebeda - přesun
    Map<String, ?> tryDb(String path, String name);

    /**
     * Load model for one book item.
     *
     * @param path path to store directory
     * @param name name of book file without suffix
     * @return part of model for page render
     */
    Map<String, Object> loadItemModel(String path, String name);

}
