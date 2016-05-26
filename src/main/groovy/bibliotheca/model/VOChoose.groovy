package bibliotheca.model

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils
/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 7.12.15
 */
@ToString
@EqualsAndHashCode
public class VOChoose {
    String urlimg;
    String url;
    String title;
    String other;

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

}
