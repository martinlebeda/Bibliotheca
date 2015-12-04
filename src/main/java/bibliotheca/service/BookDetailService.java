package bibliotheca.service;

import bibliotheca.model.VOFile;
import bibliotheca.model.VOFileDetail;

import java.util.List;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 4.12.15
 */
public interface BookDetailService {

    /**
     * Create object with detail file description
     *
     * @param path       path for load
     * @param key        name of book files without suffix
     * @param voFileList list of files loaded from path (usualy loaded once for all books in path)
     * @return book description
     */
    VOFileDetail getVoFileDetail(String path, String key, List<VOFile> voFileList);

    /**
     * Create object with detail file description.
     * List of files in path will be loaded internally.
     *
     * @param path       path for load
     * @param key        name of book files without suffix
     * @return book description
     */
    VOFileDetail getVoFileDetail(String path, String key);

    /**
     * Create target path for fiction books.
     * For base of path use "fictionArchive" from config.
     *
     * @param author author in schema "surrname, firstname [middlenames...]"
     * @return path for move book
     */
    String getTgtPathByAuthor(String author);
}
