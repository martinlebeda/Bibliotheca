package bibliotheca.tools

import bibliotheca.model.VOFile
import bibliotheca.model.VOPath
import lombok.SneakyThrows
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils
import org.yaml.snakeyaml.Yaml

import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.file.Paths
import java.util.stream.Collectors
/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 21.8.14
 */
public class Tools {

    public static final int THUMBNAIL_SIZE = 200;
    public static final String NOCOVER = "nocover";
    public static final String PARAM_PATH = "path";
    public static final String FRM_SEARCH = "booksearch";

    public static final String METADATA_KEY_DATABAZEKNIH_CZ = "dbknih";
    public static final String METADATA_KEY_DATABAZEKNIH_CZ_HODNOCENI_PROCENTO = "dbknihHodnoceniProcento";
    public static final String METADATA_KEY_DATABAZEKNIH_CZ_HODNOCENI_POCET = "dbknihHodnoceniPocet";
    public static final String METADATA_KEY_NAZEV = "nazev";
    public static final String METADATA_KEY_SERIE = "serie";
    public static final String METADATA_KEY_POZNAMKA = "poznamka";
    public static final String METADATA_KEY_AUTHORS = "authors";

    /**
     * Check if client coming from localhost
     *
     * @return true if client coming from localhost
     */
    public static boolean isLocalHost() {
//        final String ip = request.ip();
//        return "127.0.0.1".equals(ip);
        return false;
    }

    /**
     * @param title title of page
     * @param path path of browse page
     * @return base of model for render page
     */
    @SneakyThrows
    public static HashMap<String, Object> getDefaultModel(final String title, String path) {
        final HashMap<String, Object> model = new HashMap<>();
        model.put("title", title);
        model.put("isLocal", isLocalHost());

        if (StringUtils.isNotBlank(path)) {
            model.put(Tools.PARAM_PATH, path);
            model.put("encodedPath", URLEncoder.encode(path, "UTF-8").replaceAll("%2F", "/"));
        }

        return model;
    }

    public static String getThumbnailName(File file) {
        return file.getParent() + File.separator + FilenameUtils.getBaseName(file.getAbsolutePath()) + ".jpg";
    }

    @SneakyThrows
    public static File renameDirectory(final File file, final String frmName) {
        final File newPath;
        newPath = Paths.get(file.getParentFile().getAbsolutePath(), frmName).toFile();
        if (newPath.exists()) {
            // move content and delete
            final File[] files = file.listFiles();
            if (ArrayUtils.isNotEmpty(files)) {
                Arrays.stream(files).forEach({srcFile -> moveFileToNewPath(newPath, srcFile)});
            }
            FileUtils.deleteDirectory(file);
        } else {
            FileUtils.moveDirectory(file, newPath);
        }
        return newPath;
    }

    @SneakyThrows
    private static void moveFileToNewPath(File newPath, File srcFile) {
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
    }

    @SneakyThrows
    public static String writeDescription(final String path, String name, final String frmDescription) {
        String baseFileName = Paths.get(path, name).toString();
        final String desc;
        final File readme = new File(baseFileName + ".mkd");
        final FileOutputStream outputStream = new FileOutputStream(readme);
        IOUtils.write(frmDescription, outputStream);
        outputStream.close();
        desc = frmDescription;
        return desc;
    }

    @SneakyThrows
    public static VOFile downloadCover(final String baseFileName, final String frmCover) {
        final VOFile cover;
        URL website = new URL(frmCover);
        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        String covername = baseFileName + ".jpg";
        FileOutputStream fos = new FileOutputStream(covername);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        cover = new VOFile(covername);

        File nocoverfile = new File(baseFileName + ".nocover");
        if (nocoverfile.exists()) {
            nocoverfile.delete();
        }
        return cover;
    }

    public static void renameAll(final String basename, final File[] listFiles, final String frmName) {
        // rename all
        final String finalBasename = basename;
        Arrays.stream(listFiles).forEach({file1 ->
            renameFile(frmName, finalBasename, file1);
        });
    }

    @SneakyThrows
    private static void renameFile(String frmName, String finalBasename, File file1) {
        String path1 = file1.getAbsolutePath();
        String path2 = file1.getAbsolutePath().replace(finalBasename, frmName);
        final File srcFile = new File(path1);
        final File destFile = new File(path2);
        if (destFile.exists()) {
            final String s = destFile.getAbsolutePath() + "." + DigestUtils.sha1Hex(new FileInputStream(destFile));
            destFile.renameTo(new File(s));
        }
        FileUtils.moveFile(srcFile, destFile);
    }

    @SneakyThrows
    public static Map<String, Object> loadMetaData(String path, String basename) {
        final File file = Paths.get(path, basename + ".yaml").toFile();
        if (file.exists()) {
            InputStream input = new FileInputStream(file);
            Yaml yaml = new Yaml();
            return (Map<String, Object>) yaml.load(input);
        } else {
            return new HashMap<>();
        }
    }

    public static Map<String, Object> getStringStringMap(String path, String basename) {
        Map<String, Object> metadata = loadMetaData(path, basename);
        if (!metadata?.containsKey(METADATA_KEY_DATABAZEKNIH_CZ)) {
            metadata.put(METADATA_KEY_DATABAZEKNIH_CZ, "");
        }
        return metadata;
    }

    // TODO - JavaDoc - Lebeda
    public static List<VOPath> getVoPaths(String path) {
        File[] files = new File(path).listFiles();
        return Arrays.asList(files).stream()
                .filter({it.isFile()})
                .map({new VOPath(it)})
                .collect(Collectors.toList());
    }
}
