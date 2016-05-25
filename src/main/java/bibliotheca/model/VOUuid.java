package bibliotheca.model;

import lombok.Data;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 4.12.15
 */
@Data
public class VOUuid {
    private String uuid;
    private String path;
    private String name;

    public VOUuid() {
        super();
    }

    public VOUuid(String name, String path, String uuid) {
        this.name = name;
        this.path = path;
        this.uuid = uuid;
    }

}
