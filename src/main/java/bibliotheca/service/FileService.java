package bibliotheca.service;

import bibliotheca.model.VOFile;

import java.io.File;
import java.util.HashMap;
import java.util.List;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 4.12.15
 */
public interface FileService {

    /**
     * Load cover for book or return null if exists .nocover file.
     * If not loaded from disk (.jpg), try extract it from other files.
     * If not extracted, save .nocover file as semafor for next load and return null.
     *
     * @param files for book
     * @return cover for book
     */
    VOFile getCover(final List<VOFile> files);

    /**
     * Load or try generate file of desired type.
     * For conversion use calibre and open office from config.
     *
     * @param files list of files of book
     * @param suffix desired suffix (= type)
     * @param generate generate automatically if not exists
     * @return file of desired type
     */
    VOFile getTypeFile(List<VOFile> files, final String suffix, final boolean generate);

    /**
     * fill data for navigator in header of UI pages
     *
     * @param model
     * @param file
     * @param navigableLastFile
     *
     * @deprecated will be changed interface to more clear
     */
    @Deprecated
    void fillNavigatorData(final HashMap<String, Object> model,
                                      final File file,
                                      final boolean navigableLastFile);

    /**
     * Refresh files of book from disk (ie. after some files generated).
     *
     * @param basename name of book (filename without path and suffix)
     * @param file path to book
     * @param voFileList old file list
     * @return comlete list with new items
     *
     * @deprecated must be more clearly - unreadable
     */
    @Deprecated
    File[] refreshFiles(final String basename, final File file, List<VOFile> voFileList);

    // TODO - JavaDoc - Lebeda
    public void tidyUp(final File fileFrom, final File tgtFile);

    // TODO - JavaDoc - Lebeda
    void tidyUpBook(String name, String path);
}
