package bibliotheca.model;

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
