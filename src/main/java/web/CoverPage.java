package web;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;

import config.VOConfig;
import spark.Request;
import spark.Response;


/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 16.12.14
 */
public class CoverPage extends AbstractPage {

    public CoverPage(final VOConfig config, final Request request, final Response response) {
        super(config, request, response);
    }

    @Override
    public Map<String, Object> getModel() {
        File file = new File(request.queryMap(PARAM_PATH).value());

        try {
            response.type("image/jpeg");
            response.header("Content-disposition", "attachment;filename=\""+file.getName()+"\"");

            BufferedImage image = ImageIO.read(file);
            BufferedImage thumbnail = Scalr.resize(image, THUMBNAIL_SIZE);
            ImageIO.write(thumbnail, "jpg", response.raw().getOutputStream());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return new HashMap<>();
    }
}
