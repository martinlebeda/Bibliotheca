package bibliotheca.model;

import java.io.File;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 20.12.15
 */
public class VODirData {
    private String path;
    private String title;

    public VODirData(File file) {
        this.path = file.getAbsolutePath();
        this.title = file.getName();
    }

    public String getPath() {
        return path;
    }

    public String getTitle() {
        return title;
    }
}
