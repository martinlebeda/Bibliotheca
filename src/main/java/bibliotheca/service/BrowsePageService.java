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

    // TODO - JavaDoc - Lebeda
    Map<String, ?> tryDb(String uuid);
}
