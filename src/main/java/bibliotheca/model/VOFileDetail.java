package bibliotheca.model;

import bibliotheca.config.VODevice;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;



/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 16.12.14
 */
public class VOFileDetail {
    private final String name;
    private final String cover;
    private final String dbknihUrl;
    private final List<VOFile> files = new ArrayList<>();
    private final String desc;
    private final List<String> targets = new ArrayList<>();
    private List<VODevice> devices = new ArrayList<>();

    private boolean tidyUp = false;
    private String targetPath = "";
    private String author;

    private final String nazev;
    private final List<String> authors = new ArrayList<>();
    private final String serie;
    //    private final String hodnoceniDb;
//    private final int hodnoceni;

    public VOFileDetail(final String name, final String cover, final String desc, final String dbknihUrl,
                        final String nazev, final String serie, final List<String> authors) {
        this.desc = desc;
        this.cover = StringUtils.defaultString(cover);
        this.name = StringUtils.defaultString(name);
        this.dbknihUrl = dbknihUrl;

        this.nazev = (StringUtils.isBlank(nazev) ? getBookname() : nazev);
        this.serie = (StringUtils.isBlank(nazev) ? getBookserie() : nazev);

        if (CollectionUtils.isNotEmpty(authors)) {
            this.authors.addAll(authors);
        } else {
            this.authors.add(getBookauthor());
        }
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

    private String getBookauthor() {
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

}
