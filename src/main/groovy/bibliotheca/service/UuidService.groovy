package bibliotheca.service

import bibliotheca.model.VOUuid
import com.google.gson.Gson
import groovy.util.logging.Log
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.mapdb.*
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import javax.validation.constraints.NotNull
import java.nio.file.Paths
import java.util.concurrent.ConcurrentMap
/**
 * Routines for manipulate with uuid of books.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 4.12.15
 */
@Service
@Log
public class UuidService implements DisposableBean {

    @Autowired
    private ConfigService configService;

    /**
     * Cache for books uuid
     */
    private DB db;
    private boolean refreshRunning = false;

    @Delegate
    private ConcurrentMap<String, VOUuid> indexCache;

    @PostConstruct
    void init() {

        db = DBMaker.fileDB(Paths.get(configService.getBibliothecaDir(),"indexCache.db").toString()).make();
        indexCache = db.hashMap("indexCache", Serializer.STRING, new Serializer<VOUuid>() {
                @Override
                public void serialize(@NotNull DataOutput2 out, @NotNull VOUuid value) throws IOException {
                    Gson gson = new Gson();
                    out.writeChars(gson.toJson(value));
                }

                @Override
                public VOUuid deserialize(@NotNull DataInput2 input, int available) throws IOException {
                    def line = input?.readLine()
                    VOUuid json = null
                    if (line) {
                        Gson gson = new Gson();
                        json = gson.fromJson(line, VOUuid.class)
                    }
                    return json;
                }
            }).createOrOpen();
    }

    /**
     * Load from disk (.uuid file) or generate new uuid for book
     *
     * @param path path where book is stored
     * @param key  name of book (filename without path and suffix)
     * @return uuid for this book
     */
    public String getUuid(String path, String key) {
        // get or create uuid for book
        final String result;
        File file = Paths.get(path, key + ".uuid").toFile();
        if (file.exists()) {
            result = file.readLines().first()
        } else {
            result = createNewUUID(file);
        }

        // store in cache
        indexCache.put(result, new VOUuid(key, path, result));

        return result;
    }

    /**
         * Find VO by uuid in cache.
         *
         * @param uuid uuid of book
         * @return VO by uuid
         */
    public VOUuid getByUuid(String uuid) {
        VOUuid voUuid = indexCache.get(uuid);

        // check existence value
        voUuid = checkUuidFileExists(voUuid)

        return voUuid;
    }

    private def checkUuidFileExists(VOUuid voUuid) {
        final File file = Paths.get(voUuid.getPath(), voUuid.getName() + ".uuid").toFile();
        if (!file.exists()) {
            removeFromCache(voUuid.uuid);
            voUuid = null;
            Thread.start { refreshUuids() }
        }
        return voUuid
    }

    /**
         * Remove ID from cache.
         * ie. if book is deleted or joined to another.
         * @param id id of book
         */
    public void removeFromCache(String id) {
        indexCache.remove(id);
        log.info("$id is removed from uuid index cache")
    }

    // TODO - JavaDoc - Lebeda
    public void refreshUuids() {
        if (refreshRunning) {
            log.info("Full refresh is running - skipping request")
            return
        }

        refreshRunning = true;
        try {
            log.info("Full refresh of uuid cache started")
            def fictionArchive = new File(configService.getConfig().getFictionArchive())
            def map = new HashMap<String, VOUuid>()

            fictionArchive.eachFileRecurse { File f ->
                if (f.name.endsWith("uuid") && f.isFile()) {
                    def name = FilenameUtils.getBaseName(f.name)
                    def uuid = getUuid(f.parent, name);
                    map.put(uuid, new VOUuid(name, f.parent, uuid))
                }
            }

            indexCache.clear()
            indexCache.putAll(map)

            log.info("Full refresh of uuid cache ended with ${indexCache.size()} items")
        } finally {
            refreshRunning = false
        }
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
