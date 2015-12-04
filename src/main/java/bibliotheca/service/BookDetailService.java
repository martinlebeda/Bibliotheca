package bibliotheca.service;

import bibliotheca.model.VOFile;
import bibliotheca.model.VOFileDetail;

import java.util.List;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 4.12.15
 */
public interface BookDetailService {

    // TODO - JavaDoc - Lebeda
    VOFileDetail getVoFileDetail(String path, String key, List<VOFile> voFileList);

    // TODO - JavaDoc - Lebeda
    String getTgtPathByAuthor(String author);
}
