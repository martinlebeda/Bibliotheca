package bibliotheca.service.impl;

import bibliotheca.config.ConfigService;
import bibliotheca.model.VOUuid;
import bibliotheca.service.UuidService;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.mapdb.*;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 4.12.15
 */
@Service
public class UuidServiceImpl implements UuidService, DisposableBean {

    @Autowired
    private ConfigService configService;

    /**
     * Cache for books uuid
     */
    private Gson gson = new Gson();
    private DB db;
    private ConcurrentMap<String, VOUuid> indexCache;

    @PostConstruct
    void init() {
        db = DBMaker.fileDB(Paths.get(configService.getBibliothecaDir(),"indexCache.db").toString()).make();
        indexCache = db.hashMap("indexCache", Serializer.STRING, new Serializer<VOUuid>() {
                @Override
                public void serialize(@NotNull DataOutput2 out, @NotNull VOUuid value) throws IOException {
                    out.writeChars(gson.toJson(value));
                }

                @Override
                public VOUuid deserialize(@NotNull DataInput2 input, int available) throws IOException {
                    return gson.fromJson(input.readLine(), VOUuid.class);
                }
            }).createOrOpen();
    }

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
        VOUuid voUuid = indexCache.get(uuid);

        // check existence value
        final File file = Paths.get(voUuid.getPath(), voUuid.getName() + ".uuid").toFile();
        if (!file.exists()) {
            voUuid = null;
            removeFromCache(uuid);
            // TODO Lebeda - start refresh cache on background, depends on semafor
        }

        return voUuid;
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

    @Override
    public void destroy() throws Exception {
        db.close();
    }
}
