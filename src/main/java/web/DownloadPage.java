package web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import config.VOConfig;
import spark.Request;
import spark.Response;


/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 16.12.14
 */
public class DownloadPage extends AbstractPage {

    public DownloadPage(final VOConfig config, final Request request, final Response response) {
        super(config, request, response);
    }

    @Override
    public Map<String, Object> getModel() {
        File file = new File(request.queryMap(PARAM_PATH).value());

        try {
            if (isLocalHost()) {
                List<String> localExe = new ArrayList<>();
                localExe.add("exo-open");
                localExe.add(file.getAbsolutePath());
                Process child = Runtime.getRuntime().exec(localExe.toArray(new String[localExe.size()]));
                if (file.isDirectory()) {
                    final String encode = URLEncoder.encode(file.getAbsolutePath(), "UTF-8");
                    response.redirect("browse?path=" + encode);
                } else {
                    response.redirect("browse?path=" + URLEncoder.encode(file.getParent(), "UTF-8"));
                }
            } else {
                response.type("application/octet-stream");
                response.header("Content-disposition", "attachment;filename=\"" + file.getName() + "\"");
                IOUtils.copy(new FileInputStream(file), response.raw().getOutputStream());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return new HashMap<>();
    }
}
