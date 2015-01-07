package web.browse;

import org.apache.commons.io.FilenameUtils;


/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 16.12.14
 */
public class VOFile {
    private final String name;
    private final String path;
    private final String ext;

    public VOFile(final String ext, final String name, final String path) {
        this.ext = ext;
        this.name = name;
        this.path = path;
    }

    public VOFile(final String path) {
        this.path = path;
        this.ext = FilenameUtils.getExtension(path);
        this.name = FilenameUtils.getName(path);
    }

    public String getExt() {
        return ext;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }
}
