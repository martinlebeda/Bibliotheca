package bibliotheca.service.impl;

import bibliotheca.model.VOUuid;
import bibliotheca.service.UuidService;
import bibliotheca.tools.Tools;
import org.apache.commons.io.IOUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 4.12.15
 */
@Service
public class UuidServiceImpl implements UuidService {

    // TODO - JavaDoc - Lebeda
    private Map<String, VOUuid> indexCache = new HashMap<>();

    @Override
    public String getUuid(String path, String key) {
        // get or create uuid for book
        final String result;
        File file = Paths.get(path, key + ".uuid").toFile();
        try {
            if (file.exists()) {
                result = IOUtils.readLines(new FileReader(file)).get(0);
            } else {
                result = createNewUUID(file);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        // store in cache
        indexCache.put(result, new VOUuid(key, path, result));

        return result;
    }

    // TODO - JavaDoc - Lebeda
    private static String createNewUUID(File file) throws IOException {
        String result;
        result = UUID.randomUUID().toString();
        FileWriter output = new FileWriter(file);
        IOUtils.write(result, output);
        output.close();
        return result;
    }

    // TODO - JavaDoc - Lebeda
    @Scheduled(fixedDelay = Tools.CLEAR_CACHE_DELAY)
    public void clearCache() {
        List<VOUuid> tmpVoUuidList = new ArrayList<>(indexCache.values());
        tmpVoUuidList.forEach(voUuid -> {
            long distance = (new Date()).getTime() - voUuid.getCached().getTime();
            if (distance > Tools.CLEAR_CACHE_DELAY) {
                indexCache.remove(voUuid.getUuid());
            }
        });
    }

}
