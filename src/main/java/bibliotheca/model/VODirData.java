package bibliotheca.model;

import lombok.Data;

import java.io.File;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 20.12.15
 */
@Data
public class VODirData {
    private String path;
    private String title;

    public VODirData(File file) {
        this.path = file.getAbsolutePath();
        this.title = file.getName();
    }

}
