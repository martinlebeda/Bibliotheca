package bibliotheca.model

import groovy.transform.Canonical
/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 20.12.15
 */
// TODO Lebeda - move to groovy
@Canonical
public class VODirData {
    String path;
    String title;

    public VODirData(File file) {
        this.path = file.getAbsolutePath();
        this.title = file.getName();
    }

}
