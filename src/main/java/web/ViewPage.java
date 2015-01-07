package web;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;

import config.VOConfig;
import spark.Request;
import spark.Response;
import web.browse.VOFile;


/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 18.12.14
 */
public class ViewPage extends AbstractPage{

    public ViewPage(final VOConfig config, final Request request, final Response response) {
        super(config, request, response);
    }

    @Override
    public Map<String, Object> getModel() {
        return null;
    }

    public Object getData() {
        final String splat = URLDecoder.decode(request.splat()[0]);

        final String[] strings = StringUtils.splitByWholeSeparator(splat, "/pack/");
        final String baseName = File.separator + strings[0];
        final String fileName = strings[1];

        File baseFile = new File(baseName);

        final List<VOFile> files = new ArrayList<>();
        refreshFiles(FilenameUtils.getBaseName(baseFile.getName()), baseFile.getParentFile(), files);
        VOFile voFile = getTypeFile(files, "htmlz", true);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (voFile != null) {
            try {
                ZipFile zip = new ZipFile(baseFile);
                ZipEntry entry = zip.getEntry(fileName);
                if (entry != null) {
                    InputStream is = zip.getInputStream(entry);
                    IOUtils.copy(is, output);
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        ContentInfoUtil util = new ContentInfoUtil();
        ContentInfo info = util.findMatch(output.toByteArray());

        if ((info != null) && !info.getMimeType().startsWith("text")) {
            response.type(info.getMimeType());
            response.header("Content-disposition", "attachment;filename=\"${file.name}\"");
            try {
                IOUtils.write(output.toByteArray(), response.raw().getOutputStream());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return "";
        } else {
            return output.toString();
        }
    }
}
