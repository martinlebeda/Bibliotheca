package web.editfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import config.VOConfig;
import spark.Request;
import spark.Response;
import web.AbstractPage;
import web.browse.VOFile;


/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 17.12.14
 */
public class EditFilePage extends AbstractPage {

    public EditFilePage(final VOConfig config, final Request request, final Response response) {
        super(config, request, response);
    }

    @Override
    public Map<String, Object> getModel() {
        final HashMap<String, Object> model = getDefaultModel("Bibliotheca - edit file");

        String basename = request.queryMap("basename").value();

        File file = new File(request.queryMap(PARAM_PATH).value());
        model.put(PARAM_PATH, file.getAbsolutePath());


        List<VOFile> voFileList = new ArrayList<>();
        final String finalBasename2 = basename;
        File[] listFiles = file.listFiles(pathname -> {
            return pathname.getName().startsWith(finalBasename2);
        });
        Arrays.stream(listFiles).forEach(file1 -> voFileList.add(new VOFile(file1.getAbsolutePath())));

        // rename if need
        String frmName = request.queryMap(FRM_NAME).value();
        if (StringUtils.isNoneBlank(frmName) && !basename.equals(frmName)) {
            renameAll(basename, listFiles, frmName);

            // refresh names
            basename = frmName;
            listFiles = refreshFiles(basename, file, voFileList);
        }

        // download new cower if need
        VOFile cover = getCover(voFileList);
        String baseFileName = Paths.get(file.getAbsolutePath(), basename).toString();
        String frmCover = request.queryMap(FRM_COVER).value();
        if (StringUtils.isNoneBlank(frmCover)) {
            cover = downloadCover(baseFileName, frmCover);
        }

        // create description if need
        String desc = getRawDesc(voFileList);
        String frmDescription = request.queryMap(FRM_DESCRIPTION).value();
        if (StringUtils.isNoneBlank(frmDescription) && (!frmDescription.equals(desc))) {
            desc = createDescription(baseFileName, frmDescription);
        }

        fillNavigatorData(model, listFiles[0], false);

        model.put("name", basename);
        model.put("author", file.getName());
        model.put("optnames", Arrays.stream(file.listFiles(File::isFile))
                        .map(file1 -> FilenameUtils.getBaseName(file1.getName()))
                        .collect(Collectors.toSet())
        );

        model.put("desc", desc);
        model.put("cover", (cover != null ? cover.getPath(): null));

        return model;
    }

    private static String createDescription(final String baseFileName, final String frmDescription) {
        final String desc;
        try {
        final File readme = new File(baseFileName + ".mkd");
            final FileOutputStream outputStream = new FileOutputStream(readme);
            IOUtils.write(frmDescription, outputStream);
            outputStream.close();
            desc = frmDescription;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return desc;
    }

    private static VOFile downloadCover(final String baseFileName, final String frmCover) {
        final VOFile cover;
        try {
            URL website = new URL(frmCover);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            String covername = baseFileName + ".jpg";
            FileOutputStream fos = new FileOutputStream(covername);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            cover = new VOFile(covername);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return cover;
    }



    private static void renameAll(final String basename, final File[] listFiles, final String frmName) {
        // rename all
        final String finalBasename = basename;
        Arrays.stream(listFiles).forEach(file1 -> {
            try {
                String path1 = file1.getAbsolutePath();
                String path2 = file1.getAbsolutePath().replace(finalBasename, frmName);
                final File srcFile = new File(path1);
                final File destFile = new File(path2);
                if (destFile.exists()) {
                    final String s = destFile.getAbsolutePath() + "." + DigestUtils.sha1Hex(new FileInputStream(destFile));
                    destFile.renameTo(new File(s));
                }
                FileUtils.moveFile(srcFile, destFile);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private String getRawDesc(final List<VOFile> files) {
        VOFile readme = getTypeFile(files, "mkd", false);
        final String html;
        if (readme != null) {
            final File file = new File(readme.getPath());
            try {
                final FileInputStream input = new FileInputStream(file);
                final List<String> strings = IOUtils.readLines(input);
                html = StringUtils.join(strings, "\n");
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            html = null;
        }

        return html;
    }

}
