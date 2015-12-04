package bibliotheca.service;

import java.util.Map;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 4.12.15
 */
public interface EditFilePageService {

    // TODO - JavaDoc - Lebeda
    Map<String, Object> getModel(String path, String basename, final String frmName, String frmCover, String frmDescription, String dbknih,
                                            String loadImage, String loadDescription, String loadAll, String loadAllClose, String tryDbKnih);
}
