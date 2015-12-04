package bibliotheca.model;

import bibliotheca.config.VODevice;
import bibliotheca.tools.Tools;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 16.12.14
 */
public class VOFileDetail {
    private final String name;
    private String cover;
    private final String dbknihUrl;
    private final List<VOFile> files = new ArrayList<>();
    private final String desc;
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

    public VOFileDetail(final String uuid, final String name, final String cover, final String desc, Map<String, Object> metadata) {
//        final String dbknihUrl,
//                                final String nazev, final String serie, final List<String> authors
//
//                (String) metadata.get(Tools.METADATA_KEY_DATABAZEKNIH_CZ),
//                 (String) metadata.get(Tools.METADATA_KEY_NAZEV),
//                 (String) metadata.get(Tools.METADATA_KEY_SERIE),
//                 (List<String>) metadata.get(Tools.METADATA_KEY_AUTHORS

        this.uuid = uuid;

        this.desc = desc;
        this.cover = StringUtils.defaultString(cover);
        this.name = StringUtils.defaultString(name);
        this.dbknihUrl = (String) metadata.get(Tools.METADATA_KEY_DATABAZEKNIH_CZ);

        final String nazev = (String) metadata.get(Tools.METADATA_KEY_NAZEV);
        this.nazev = (StringUtils.isBlank(nazev) ? getBookname() : nazev);

        final String serie = (String) metadata.get(Tools.METADATA_KEY_SERIE);
        this.serie = (StringUtils.isBlank(serie) ? getBookserie() : serie);

        @SuppressWarnings("unchecked")
        final List<String> authors = (List<String>) metadata.get(Tools.METADATA_KEY_AUTHORS);
        if (CollectionUtils.isNotEmpty(authors)) {
            this.authors.addAll(authors);
        } else {
            this.authors.add(getBookauthor());
        }

        this.hodnoceniDbProcento = (String) metadata.get(Tools.METADATA_KEY_DATABAZEKNIH_CZ_HODNOCENI_PROCENTO);
        this.hodnoceniDbPocet = (String) metadata.get(Tools.METADATA_KEY_DATABAZEKNIH_CZ_HODNOCENI_POCET);
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

    public String getNazev() {
        return nazev;
    }

    public String getSerie() {
        return serie;
    }

    public String getAutor() {
        return StringUtils.join(authors, "; ");
    }

    public void setNazev(String nazev) {
        this.nazev = nazev;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public String getHodnoceniDbPocet() {
        return hodnoceniDbPocet;
    }

    public void setHodnoceniDbPocet(String hodnoceniDbPocet) {
        this.hodnoceniDbPocet = hodnoceniDbPocet;
    }

    public String getHodnoceniDbProcento() {
        return hodnoceniDbProcento;
    }

    public void setHodnoceniDbProcento(String hodnoceniDbProcento) {
        this.hodnoceniDbProcento = hodnoceniDbProcento;
    }
}
