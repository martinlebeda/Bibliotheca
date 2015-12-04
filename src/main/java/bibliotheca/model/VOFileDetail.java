package bibliotheca.model;

import bibliotheca.config.VODevice;
import bibliotheca.tools.Tools;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;


/**
 * Not clearly VO, contains some simple logic.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 16.12.14
 */
public class VOFileDetail {
    private final String name; // TODO Lebeda - rename to bookFileName
    private String cover;
    private String dbknihUrl;
    private final List<VOFile> files = new ArrayList<>();
    private final String desc;
    private final String path;
    private final List<String> targets = new ArrayList<>();
    private List<VODevice> devices = new ArrayList<>();

    private boolean tidyUp = false;
    private String targetPath = "";
    private String author;

    private String nazev;
    private final List<String> authors = new ArrayList<>();
    private String serie;

    private String hodnoceniDbProcento;
    private String hodnoceniDbPocet;
//    private final int hodnoceni;

    private String uuid;
    private boolean dirty = false;

    public VOFileDetail(final String uuid, final String name, final String cover, final String desc, String path, Map<String, Object> metadata) {
        this.uuid = uuid;

        this.desc = desc;
        this.path = path;
        this.cover = StringUtils.defaultString(cover);
        this.name = StringUtils.defaultString(name);

        this.dbknihUrl = (String) metadata.get(Tools.METADATA_KEY_DATABAZEKNIH_CZ);
        this.nazev = (String) metadata.get(Tools.METADATA_KEY_NAZEV);
        this.serie = (String) metadata.get(Tools.METADATA_KEY_SERIE);
        this.hodnoceniDbProcento = (String) metadata.get(Tools.METADATA_KEY_DATABAZEKNIH_CZ_HODNOCENI_PROCENTO);
        this.hodnoceniDbPocet = (String) metadata.get(Tools.METADATA_KEY_DATABAZEKNIH_CZ_HODNOCENI_POCET);

        //noinspection unchecked
        this.authors.addAll(CollectionUtils.emptyIfNull((List <String>) metadata.get(Tools.METADATA_KEY_AUTHORS)));
    }

    public boolean isDirty() {
        return dirty;
    }

    public Map<String, Object> getMetadata() {
        Map<String, Object> meta = new HashMap<>();

        meta.put(Tools.METADATA_KEY_DATABAZEKNIH_CZ, this.dbknihUrl);
        meta.put(Tools.METADATA_KEY_NAZEV, this.nazev);
        meta.put(Tools.METADATA_KEY_SERIE, this.serie);
        meta.put(Tools.METADATA_KEY_DATABAZEKNIH_CZ_HODNOCENI_POCET, this.hodnoceniDbPocet);
        meta.put(Tools.METADATA_KEY_DATABAZEKNIH_CZ_HODNOCENI_PROCENTO, this.hodnoceniDbProcento);

        return meta;
    }

    public String getPath() {
        return path;
    }

    public String getUuid() {
        return uuid;
    }

    public String getEncodedName() {
        try {
            return URLEncoder.encode(getName(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    };

    public void setDbknihUrl(String dbknihUrl) {
        if (!StringUtils.equals(this.dbknihUrl, dbknihUrl)) {
            this.dbknihUrl = dbknihUrl;
            this.dirty = true;
        }
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public List<String> getTargets() {
        return targets;
    }

    public List<VODevice> getDevices() {
        return devices;
    }

    public String getDesc() {
        return desc;
    }

    public String getCover() {
        return cover;
    }

    public List<VOFile> getFiles() {
        return files;
    }

    public String getName() {
        return name;
    }

    public void setTidyUp(final boolean tidyUp) {
        this.tidyUp = tidyUp;
    }

    public boolean isTidyUp() {
        return tidyUp;
    }

    public void setTargetPath(final String targetPath) {
        this.targetPath = targetPath;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setAuthor(final String author) {
        this.author = author;
    }

    public String getAuthor() {
        return author;
    }

    public boolean getCoverExists() {
        return StringUtils.isNoneBlank(cover);
    }

    public String getDbknihUrl() {
        return dbknihUrl;
    }

    public boolean getDbknihUrlExists() {
        return StringUtils.isNoneBlank(dbknihUrl);
    }

    public String getBookname() {
        return StringUtils.replacePattern(name, ".*- *", "");
    }

    public String getBookserie() {
        String[] split = StringUtils.split(name, "-", 3);
        if (split.length == 3) {
            return StringUtils.trim(split[1]);
        }
        return null;
    }

    public String getBookauthor() {
        String[] split = StringUtils.split(name, "-", 2);
        return StringUtils.trim(split[0]);
    }

    /**
     * @return clear value from metadata
     */
    public String getNazevMeta() {
        return nazev;
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
    public String getNazev() {
        return (StringUtils.isBlank(nazev) ? getBookname() : nazev);
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

    public void setNazev(String nazev) {
        if (!StringUtils.equals(this.nazev, nazev)) {
           this.nazev = nazev;
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
        this.authors.addAll(authors);
        this.dirty = true;
    }
}
