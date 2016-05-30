package bibliotheca.service

import bibliotheca.ftmap.FTMap
import bibliotheca.model.VOFileDetail
import bibliotheca.model.VOUuid
import groovy.util.logging.Log
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import java.nio.file.Paths
import java.text.Normalizer
/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 * Date: 27.5.16
 */
@Service
@Log
class FtMetaService implements DisposableBean {

    @Autowired
    private ConfigService configService;

    @Autowired
    private UuidService uuidService;

    @Autowired
    private BookDetailService bookDetailService;

    @Delegate
    private FTMap db;

    @PostConstruct
    void init() {
        db = new FTMap(Paths.get(configService.getBibliothecaDir(), "ftMetadata"));
    }

    void destroy() {
        db.close()
    }

    // TODO - JavaDoc - Lebeda
    void put(VOFileDetail vo) {
        // TODO Lebeda - batch reindex
        db.put(vo.uuid, prepareValue(vo))
    }

    // TODO - JavaDoc - Lebeda
    void reindexAll() {
        log.info("Reindexing metadata started")
        def map = new HashMap<String, String>()
        for (String it in uuidService.keySet()) {

//        }
//        uuidService.keySet().stream().forEach({
            VOUuid voUuid = uuidService.get(it);
            String path = voUuid.getPath();
            String name = voUuid.getName();

            VOFileDetail fd = bookDetailService.getVoFileDetail(path, name);
            map.put(it, prepareValue(fd));
            log.info("Reindexing metadata prepared '${fd.bookFileName}'")

        }
        log.info("Reindexing metadata prepared ${map.size()} items")

        // get All metadata to map
        db.clear()
        db.putAll(map)
        log.info("Reindexing metadata ended")
    }

    Set<String> search(String booksearch) {
        db.search(removeDiacritics(booksearch.toLowerCase()), Integer.MAX_VALUE);
    }

    // TODO - JavaDoc - Lebeda
    private static String prepareValue(VOFileDetail vo) {
        def value = removeDiacritics([vo.authors.join(" "), vo.title ?: "", vo.serie ?: "", vo.desc ?: ""].join(" ").toLowerCase()).trim()
        log.fine("Prepared value: ${value}")
        return value;

    }

    // TODO - JavaDoc - Lebeda
    private static String removeDiacritics(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

}
