package bibliotheca.config;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;


/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 16.12.14
 */
@Root
public class VOConfig {

    @Element
    private Integer port;

    @Element
    private String fictionArchive;

    @Element
    private String convert;

    @Element
    private String libreoffice;

    @ElementList
    private List<String> fictionPaths = new ArrayList<>();

    @ElementList(required = false)
    private List<VODevice> devices = new ArrayList<>();

    public String getLibreoffice() {
        return libreoffice;
    }

    public void setLibreoffice(final String libreoffice) {
        this.libreoffice = libreoffice;
    }

    public List<VODevice> getDevices() {
        return devices;
    }

    public void setDevices(final List<VODevice> devices) {
        this.devices = devices;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(final Integer port) {
        this.port = port;
    }

    public List<String> getFictionPaths() {
        return fictionPaths;
    }

    public void setFictionPaths(final List<String> fictionPaths) {
        this.fictionPaths = fictionPaths;
    }

    public String getFictionArchive() {
        return fictionArchive;
    }

    public void setFictionArchive(final String fictionArchive) {
        this.fictionArchive = fictionArchive;
    }

    public String getConvert() {
        return convert;
    }

    public void setConvert(final String convert) {
        this.convert = convert;
    }
}
