package bibliotheca.config;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 4.12.15
 */
public interface ConfigService {

    /**
     * Load and provide configuration object.
     * If configuration is not loaded, automaticaly load this from disk.
     *
     * @return configuration object
     */
    VOConfig getConfig();
}
