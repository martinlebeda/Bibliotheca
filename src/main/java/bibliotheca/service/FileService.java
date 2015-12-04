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

    // TODO - JavaDoc - Lebeda
    VOFile getCover(final List<VOFile> files);

    // TODO - JavaDoc - Lebeda
    VOFile getTypeFile(List<VOFile> files, final String suffix, final boolean generate);

    // TODO - JavaDoc - Lebeda
    void fillNavigatorData(final HashMap<String, Object> model,
                                      final File file,
                                      final boolean navigableLastFile);

    // TODO - JavaDoc - Lebeda
    File[] refreshFiles(final String basename, final File file, List<VOFile> voFileList);
}
