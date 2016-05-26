package bibliotheca.model

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.apache.commons.io.FilenameUtils
/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 16.12.14
 */
// TODO Lebeda - move to groovy

@ToString
@EqualsAndHashCode
public class VOFile {
    final String name;
    final String path;
    final String ext;


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
}
