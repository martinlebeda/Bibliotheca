package bibliotheca.config;

import lombok.SneakyThrows;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 8.9.15
 */
@Service
public class ConfigServiceImpl implements ConfigService {

    // config file
    private static final String CONFIG_FILE = ".bibliotheca.xml";

    private VOConfig config = null;

    @Override
    @SneakyThrows
    public VOConfig getConfig() {
        if (config == null) {
            String homeDir = System.getProperty("user.home");
            final File configFile = Paths.get(homeDir, CONFIG_FILE).toFile();

            Serializer serializer = new Persister();
            config = serializer.read(VOConfig.class, configFile);
        }

        return config;
    }
}
