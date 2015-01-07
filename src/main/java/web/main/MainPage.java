package web.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import config.VOConfig;
import spark.Request;
import spark.Response;
import web.AbstractPage;
import web.VOPath;


/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 15.12.14
 */
public class MainPage extends AbstractPage {

    public MainPage(final VOConfig config, final Request request, final Response response) {
        super(config, request, response);
    }

    @Override
    public Map<String, Object> getModel() {
        final HashMap<String, Object> model = new HashMap<>();

        model.put("title", "Bibliotheca - Main page");

        final ArrayList<VOPath> belePaths = new ArrayList<>();
        for (String fpath : config.getFictionPaths()) {
            belePaths.add(new VOPath(FilenameUtils.getBaseName(fpath), fpath));
        }
        model.put("bele", belePaths);

        return model;
    }

}
