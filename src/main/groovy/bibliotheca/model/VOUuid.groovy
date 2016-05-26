package bibliotheca.model

import groovy.transform.Canonical
/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 4.12.15
 */
// TODO Lebeda - move to groovy

@Canonical
public class VOUuid {
    String uuid;
    String path;
    String name;

    // TODO Lebeda - smazat
    public VOUuid(String name, String path, String uuid) {
        this.name = name;
        this.path = path;
        this.uuid = uuid;
    }

}
