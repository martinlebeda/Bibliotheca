package web.editdir;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import config.VOConfig;
import spark.Request;
import spark.Response;
import web.AbstractPage;


/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 17.12.14
 */
public class EditDirPage extends AbstractPage {

    public EditDirPage(final VOConfig config, final Request request, final Response response) {
        super(config, request, response);
    }

    @Override
    public Map<String, Object> getModel() {
        final HashMap<String, Object> model = getDefaultModel("Bibliotheca - edit directory");

        File file = new File(request.queryMap(PARAM_PATH).value());
        model.put(PARAM_PATH, file.getAbsolutePath());

        // rename if need
        String frmName = request.queryMap(FRM_NAME).value();
        if (StringUtils.isNoneBlank(frmName) && !file.getName().equals(frmName)) {
            file = renameDirectory(file, frmName);
        }


        fillNavigatorData(model, file, true);

        model.put("name", file.getName());
        model.put("optnames", Arrays.stream(file.getParentFile().listFiles(File::isDirectory))
                        .map(File::getName)
                        .collect(Collectors.toList())
        );

        return model;
    }

    private static File renameDirectory(final File file, final String frmName) {
        final File newPath;
        try {
            newPath = Paths.get(file.getParentFile().getAbsolutePath(), frmName).toFile();
            if (newPath.exists()) {
                // move content and delete
                final File[] files = file.listFiles();
                if (ArrayUtils.isNotEmpty(files)) {
                    Arrays.stream(files).forEach(srcFile -> {
                                try {
                                    File tgtFile = Paths.get(newPath.getAbsolutePath(), srcFile.getName()).toFile();
                                    if (!tgtFile.exists()) {
                                        FileUtils.moveToDirectory(srcFile, newPath, false);
                                    } else {
                                        String sha1Src = DigestUtils.sha1Hex(new FileInputStream(srcFile));
                                        String sha1Tgt = DigestUtils.sha1Hex(new FileInputStream(tgtFile));

                                        if (!sha1Src.equals(sha1Tgt)) {
                                            FileUtils.copyFile(srcFile,
                                                    Paths.get(newPath.getAbsolutePath(),
                                                            sha1Src + "_" + srcFile.getName()).toFile());
                                        }
                                        //noinspection ResultOfMethodCallIgnored
                                        srcFile.delete();

                                    }
                                } catch (Exception e) {
                                    throw new IllegalStateException(e);
                                }
                            }
                    );
                }
                FileUtils.deleteDirectory(file);
            } else {
                FileUtils.moveDirectory(file, newPath);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return newPath;
    }
}
