package bibliotheca.model

import bibliotheca.config.VODevice
import bibliotheca.tools.Tools
import lombok.SneakyThrows
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
/**
 * Not clearly VO, contains some simple logic.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 16.12.14
 */
// TODO Lebeda - move to groovy

public class VOFileDetail {
    String bookFileName;
    String cover;
    String dbknihUrl;
    final List<VOFile> files = new ArrayList<>();
    String desc;
    String path;
    final List<String> targets = new ArrayList<>();
    List<VODevice> devices = new ArrayList<>();

    boolean tidyUp = false;
    String targetPath = "";
    String author;

    String title;
    final List<String> authors = new ArrayList<>();
    String serie;

    String hodnoceniDbProcento;
    String hodnoceniDbPocet;

    String uuid;
    boolean dirty = false;

    public VOFileDetail() {
        super();
    }

    public VOFileDetail(final String uuid, final String bookFileName, final String cover, final String desc, String path, Map<String, Object> metadata) {
        this.uuid = uuid;

        this.desc = desc;
        this.path = path;
        this.cover = StringUtils.defaultString(cover);
        this.bookFileName = StringUtils.defaultString(bookFileName);

        this.dbknihUrl = (String) metadata.get(Tools.METADATA_KEY_DATABAZEKNIH_CZ);
        this.title = (String) metadata.get(Tools.METADATA_KEY_NAZEV);
        this.serie = (String) metadata.get(Tools.METADATA_KEY_SERIE);
        this.hodnoceniDbProcento = (String) metadata.get(Tools.METADATA_KEY_DATABAZEKNIH_CZ_HODNOCENI_PROCENTO);
        this.hodnoceniDbPocet = (String) metadata.get(Tools.METADATA_KEY_DATABAZEKNIH_CZ_HODNOCENI_POCET);

        //noinspection unchecked
        this.authors.addAll(CollectionUtils.emptyIfNull((List <String>) metadata.get(Tools.METADATA_KEY_AUTHORS)));
    }

    public Map<String, Object> getMetadata() {
        Map<String, Object> meta = new HashMap<>();

        meta.put(Tools.METADATA_KEY_DATABAZEKNIH_CZ, this.dbknihUrl);
        meta.put(Tools.METADATA_KEY_NAZEV, this.title);
        meta.put(Tools.METADATA_KEY_SERIE, this.serie);
        meta.put(Tools.METADATA_KEY_DATABAZEKNIH_CZ_HODNOCENI_POCET, this.hodnoceniDbPocet);
        meta.put(Tools.METADATA_KEY_DATABAZEKNIH_CZ_HODNOCENI_PROCENTO, this.hodnoceniDbProcento);

        return meta;
    }

    @SneakyThrows
    public String getEncodedName() {
        return URLEncoder.encode(getBookFileName(), "UTF-8");
    }

    public void setDbknihUrl(String dbknihUrl) {
        if (!StringUtils.equals(this.dbknihUrl, dbknihUrl)) {
            this.dbknihUrl = dbknihUrl;
            this.dirty = true;
        }
    }

    public boolean getDbknihUrlExists() {
        return StringUtils.isNoneBlank(dbknihUrl);
    }

    public String getBookname() {
        return StringUtils.replacePattern(bookFileName, ".*- *", "");
    }

    public String getBookserie() {
        String[] split = StringUtils.split(bookFileName, "-", 3);
        if (split.length == 3) {
            return StringUtils.trim(split[1]);
        }
        return null;
    }

    public String getBookauthor() {
        String[] split = StringUtils.split(bookFileName, "-", 2);
        return StringUtils.trim(split[0]);
    }

    /**
     * @return clear value from metadata
     */
    public String getNazevMeta() {
        return title;
    }

    /**
     * @return clear value from metadata
     */
    public String getSerieMeta() {
        return serie;
    }

    /**
     * @return compatible value from metadata or filename
     */
    public String getTitle() {
        return (StringUtils.isBlank(title) ? getBookname() : title);
    }

    /**
     * @return compatible value from metadata or filename
     */
    public String getSerie() {
        return (StringUtils.isBlank(serie) ? getBookserie() : serie);
    }

    public String getAutor() {
        if (CollectionUtils.isNotEmpty(authors)) {
            return StringUtils.join(authors, "; ");
        } else {
            return getBookauthor();
        }
    }

    public void setTitle(String title) {
        if (!StringUtils.equals(this.title, title)) {
           this.title = title;
           this.dirty = true;
        }
    }

    public void setSerie(String serie) {
        if (!StringUtils.equals(this.serie, serie)) {
           this.serie = serie;
           this.dirty = true;
        }
    }

    public List<String> getAuthors() {
        return Collections.unmodifiableList(authors);
    }

    public String getHodnoceniDbPocet() {
        return hodnoceniDbPocet;
    }

    public void setHodnoceniDbPocet(String hodnoceniDbPocet) {
        if (!StringUtils.equals(this.hodnoceniDbPocet, hodnoceniDbPocet)) {
            this.hodnoceniDbPocet = hodnoceniDbPocet;
            this.dirty = true;
        }
    }

    public String getHodnoceniDbProcento() {
        return hodnoceniDbProcento;
    }

    public void setHodnoceniDbProcento(String hodnoceniDbProcento) {
        if (!StringUtils.equals(this.hodnoceniDbProcento, hodnoceniDbProcento)) {
            this.hodnoceniDbProcento = hodnoceniDbProcento;
            this.dirty = true;
        }
    }

    public void replaceAuthors(List<String> authors) {
        this.authors.clear();
        if (CollectionUtils.isNotEmpty(authors)) {
            this.authors.addAll(authors);
        }
        this.dirty = true;
    }
}
