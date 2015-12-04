package bibliotheca.model;

import java.util.Date;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 4.12.15
 */
public class VOUuid {
    private String uuid;
    private String path;
    private String name;
    private final Date cached = new Date();

    public VOUuid(String name, String path, String uuid) {
        this.name = name;
        this.path = path;
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getUuid() {
        return uuid;
    }

    public Date getCached() {
        return cached;
    }
}
