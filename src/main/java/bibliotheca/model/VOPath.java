package bibliotheca.model;

import java.io.File;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 16.12.14
 *
 * @deprecated nahradit pomocí File
 */
// TODO Lebeda - smazat třídu
@Deprecated
public class VOPath {
    private String path;
    private String name;

    public VOPath(final String name, final String path) {
        this.name = name;
        this.path = path;
    }

    public VOPath(File file1) {
        this.name = file1.getName();
        this.path = file1.getAbsolutePath();
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }
}
