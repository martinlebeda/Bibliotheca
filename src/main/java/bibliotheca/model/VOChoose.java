package bibliotheca.model;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 7.12.15
 */
public class VOChoose {
    private String urlimg;
    private String url;
    private String title;
    private String other;

    public VOChoose(String urlimg, String url, String title, String other) {
        this.other = other;
        this.title = title;
        this.url = url;
        this.urlimg = urlimg;
    }

    public String getAuthorSurrname() {
        String[] split = StringUtils.split(other, ", ", 2);
        String[] split1 = StringUtils.split(split[1], " ");
        return split1[split1.length - 1];
    }

    public String getAuthorFirstname() {
        String[] split = StringUtils.split(other, ", ", 2);
        String[] split1 = StringUtils.split(split[1], " ");
        String[] remove = ArrayUtils.remove(split1, split1.length - 1);
        return StringUtils.join(remove, " ");
    }

    public String getOther() {
        return other;
    }

    public void setOther(String other) {
        this.other = other;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrlimg() {
        return urlimg;
    }

    public void setUrlimg(String urlimg) {
        this.urlimg = urlimg;
    }


}
