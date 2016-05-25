package bibliotheca.service.impl;

import bibliotheca.model.VOUuid;
import bibliotheca.service.UuidService;
import bibliotheca.tools.Tools;
import lombok.SneakyThrows;
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

    /**
     * Cache for books uuid
     */
    // TODO Lebeda - reindex whole Store
    // TODO Lebeda - pernament cache mapdb??
    private Map<String, VOUuid> indexCache = new HashMap<>();

    @Override
    @SneakyThrows
    public String getUuid(String path, String key) {
        // get or create uuid for book
        final String result;
        File file = Paths.get(path, key + ".uuid").toFile();
        if (file.exists()) {
            result = IOUtils.readLines(new FileReader(file)).get(0);
        } else {
            result = createNewUUID(file);
        }

        // store in cache
        indexCache.put(result, new VOUuid(key, path, result));

        return result;
    }

    @Override
    public VOUuid getByUuid(String uuid) {
        // TODO Lebeda - check existence value
        return indexCache.get(uuid);
    }

    @Override
    public void removeFromCache(String id) {
        indexCache.remove(id);
    }

    /**
     * Create new UUID for book and store it to uuid file for next use.
     *
     * @param file the uuid file - "name of book file.uuid", if exists, will be overriden
     * @return new uuid
     * @throws IOException if cannot write uuid file
     */
    private static String createNewUUID(File file) throws IOException {
        String result;
        result = UUID.randomUUID().toString();
        FileWriter output = new FileWriter(file);
        IOUtils.write(result, output);
        output.close();
        return result;
    }

    /**
     * Periodicaly cleaning cache from old records.
     * Remove from map records older then Tools.CLEAR_CACHE_DELAY.
     */
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
