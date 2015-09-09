package bibliotheca.tools;

import bibliotheca.model.VOFile;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;


/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 21.8.14
 */
public class Tools {

    public static final int THUMBNAIL_SIZE = 200;
    public static final String NOCOVER = "nocover";
    public static final String PARAM_PATH = "path";
    public static final String FRM_SEARCH = "booksearch";

    public static String removeDiacritics(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    /**
     * Check if client coming from localhost
     * @return true if client coming from localhost
     */
    public static boolean isLocalHost() {
//        final String ip = request.ip();
//        return "127.0.0.1".equals(ip);
        return false;
    }

    public static HashMap<String, Object> getDefaultModel(final String title) {
        final HashMap<String, Object> model = new HashMap<>();
        model.put("title", title);
        model.put("isLocal", isLocalHost());
        return model;
    }

    public static String getThumbnailName(File file) {
        return file.getParent() + File.separator + FilenameUtils.getBaseName(file.getAbsolutePath()) + ".jpg";
    }

    public static File renameDirectory(final File file, final String frmName) {
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

    public static String createDescription(final String baseFileName, final String frmDescription) {
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

    public static VOFile downloadCover(final String baseFileName, final String frmCover) {
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

    public static void renameAll(final String basename, final File[] listFiles, final String frmName) {
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
}