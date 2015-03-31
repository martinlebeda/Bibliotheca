package web;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import config.VOConfig;
import spark.Request;
import spark.Response;
import web.browse.VOFile;


/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 16.12.14
 */
public abstract class AbstractPage {

    protected final VOConfig config;
    protected final Request request;
    protected final Response response;

    public static final int THUMBNAIL_SIZE = 200;
    protected static final String NOCOVER = "nocover";

    public static final String FRM_SEARCH = "booksearch";
    public static final String FRM_NAME = "bookname";
    public static final String FRM_COVER = "bookcover";
    public static final String FRM_DESCRIPTION = "bookdescription";

    public static final String TIDYUP = "tidyup";
    public static final String PARAM_PATH = "path";
    
    public AbstractPage(final VOConfig config, final Request request, final Response response) {
        this.config = config;
        this.request = request;
        this.response = response;
    }

    public abstract Map<String, Object> getModel();


    /**
     * Check if client coming from localhost
     * @return true if client coming from localhost
     */
    protected boolean isLocalHost() {
        final String ip = request.ip();
        return "127.0.0.1".equals(ip);
    }

    protected HashMap<String, Object> getDefaultModel(final String title) {
        final HashMap<String, Object> model = new HashMap<>();
        model.put("title", title);
        model.put("isLocal", isLocalHost());
        return model;
    }

    protected void fillNavigatorData(final HashMap<String, Object> model,
                                     final File file,
                                     final boolean navigableLastFile) {
        final ArrayList<VOPath> navigator = new ArrayList<>();
        if (!CollectionUtils.exists(config.getFictionPaths(), s -> s.equalsIgnoreCase(file.getAbsolutePath()))) {
            for (File navigatorFile : getNavigatorData(file.getParentFile())) {
                navigator.add(new VOPath(navigatorFile.getName(), navigatorFile.getAbsolutePath()));
            }
        }
        model.put("navigator", navigator);
        model.put("navigatorFile", FilenameUtils.getBaseName(file.getName()));
        model.put("navigatorPath", file.getAbsolutePath());
        model.put("navigableLastFile", navigableLastFile);
    }

    protected File[] refreshFiles(final String basename, final File file, List<VOFile> voFileList) {
        final File[] listFiles;
        listFiles = file.listFiles((dir, name1) -> name1.startsWith(FilenameUtils.getBaseName(basename)));
        voFileList.clear();
        Arrays.stream(listFiles).forEach(file1 -> voFileList.add(new VOFile(file1.getAbsolutePath())));
        return listFiles;
    }

    protected VOFile getTypeFile(List<VOFile> files, final String suffix, final boolean generate) {
        final List<VOFile> select = new ArrayList<>();
        select.addAll(CollectionUtils.select(files, voFile -> voFile.getExt().equalsIgnoreCase(suffix)));
        VOFile voFile = null;
        if (CollectionUtils.isNotEmpty(select)) {
            voFile = select.get(0);
        }

        if ((voFile == null) && generate) {
            generateFile(files, suffix);

            // refresh
            refreshFiles(files.get(0).getName(), new File(files.get(0).getPath()).getParentFile(), files);
            voFile = getTypeFile(files, suffix, false);
        }

        return voFile;
    }

    private void generateFile(final List<VOFile> files, final String suffix) {
        VOFile srcFile = getTypeFile(files, "htmlz", false);
        if (srcFile == null) { srcFile = getTypeFile(files, "epub", false); }
        if (srcFile == null) { srcFile = getTypeFile(files, "mobi", false); }
        if (srcFile == null) { srcFile = getTypeFile(files, "azw3", false); }
        if (srcFile == null) { srcFile = getTypeFile(files, "azw", false); }
        if (srcFile == null) { srcFile = getTypeFile(files, "rtf", false); }
        if (srcFile == null) { srcFile = getTypeFile(files, "odt", false); }
        if (srcFile == null) { srcFile = getTypeFile(files, "docx", false); }
        if (srcFile == null) { srcFile = getTypeFile(files, "prc", false); }
        if (srcFile == null) { srcFile = getTypeFile(files, "pdb", false); }
        if (srcFile == null) { srcFile = getTypeFile(files, "txt", false); }

        // podpota pro doc
        if (srcFile == null) {
            srcFile = getDocxFromDocFile(files);
        }


        if (srcFile != null) {
            // generate
            try {
                String author = null;
                String title = null;
                String cover = null;

                final String[] split = StringUtils.split(FilenameUtils.getBaseName(srcFile.getName()), "-", 2);
                if (split.length > 1) {
                    author = StringUtils.trim(split[0]);
                    title = StringUtils.trim(split[1]);
                } else {
                    title = srcFile.getName();
                }

                VOFile coverFile = getTypeFile(files, "jpg", false);
                if (coverFile != null) {
                    cover = coverFile.getPath();
                }

                final List<String> command = new ArrayList<>();
                command.add(config.getConvert());
                command.add(srcFile.getPath());
                command.add("." + suffix);

                if (srcFile.getPath().toLowerCase().endsWith(".pdb")
                        || srcFile.getPath().toLowerCase().endsWith(".txt")) {
                    command.add("--input-encoding=cp1250 ");
                }

                if (StringUtils.isNotBlank(cover)) {
                    command.add("--cover");
                    command.add(cover);
                }

                if (StringUtils.isNotBlank(author)) {
                    command.add("--authors");
                    command.add(author);
                }

                if (StringUtils.isNotBlank(title)) {
                    command.add("--title");
                    command.add(title);
                }

                ProcessBuilder builder = new ProcessBuilder(command);
                builder.directory(new File(srcFile.getPath()).getParentFile());
                final Process process = builder.start();
                process.waitFor();

                //            //~/bin/fixEpubJustify.sh "$TGT" && \
                //            //~/bin/fixEpubFont.sh "$TGT"
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private VOFile getDocxFromDocFile(final List<VOFile> files) {
        final VOFile srcFile;
        final VOFile doc = getTypeFile(files, "doc", false);
        if (doc != null) {
            try {
                final List<String> command = new ArrayList<>();
                command.add(config.getLibreoffice());
                command.add("--headless");
                command.add("--convert-to");
                command.add("docx");
                command.add(doc.getPath());

                ProcessBuilder builder = new ProcessBuilder(command);
                builder.directory(new File(doc.getPath()).getParentFile());
                final Process process;
                process = builder.start();
                process.waitFor();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        // refresh
        refreshFiles(files.get(0).getName(), new File(files.get(0).getPath()).getParentFile(), files);
        srcFile = getTypeFile(files, "docx", false);
        return srcFile;
    }

    private List<File> getNavigatorData(File file) {
        List<File> parentFiles = new ArrayList<>();
        String parentName = file.getAbsolutePath();
        while (parentName.length() > 1) {
            final String finalParentName = parentName;
            Boolean end = CollectionUtils.exists(config.getFictionPaths(), s -> s.equalsIgnoreCase(finalParentName));
            File parent = new File(parentName);
            parentFiles.add(parent);
            parentName = parent.getParent();
            if (end) {
                break;
            }
        }
        Collections.reverse(parentFiles);
        return parentFiles;
    }

    protected VOFile getCover(final List<VOFile> files) {
        VOFile cover = getTypeFile(files, "jpg", false);

        if (getTypeFile(files, NOCOVER, false) == null) {
            if (cover == null) {
                cover = generateCoverByDocx(files);
            }

            if (cover == null) {
                cover = generateCoverByEpub(files);
            }

            if (cover == null) {
                cover = generateCoverByPdf(files);
            }

            if (cover == null) {
                cover = generateCoverByDocxAnyImage(files);
            }

            if (cover == null) {
                getDocxFromDocFile(files);
                cover = generateCoverByDocx(files);
                if (cover == null) {
                    cover = generateCoverByDocxAnyImage(files);
                }
            }
        }

        return cover;
    }

    private VOFile generateCoverByDocxAnyImage(final List<VOFile> files) {
        VOFile voFile = getTypeFile(files, "docx", false);
        VOFile result = null;
        try {
            if (voFile != null) {
                File file = new File(voFile.getPath());
                ZipFile zip = new ZipFile(file);

                ZipEntry entry = null;
                final Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry zipEntry = entries.nextElement();
                    final String aCase = zipEntry.getName().toLowerCase();
                    if (aCase.endsWith("jpeg") || aCase.endsWith("jpg")) {
                        entry = zipEntry;
                        break;
                    }
                }

                if (entry != null) {
                    InputStream is = zip.getInputStream(entry);

                    String thumbnailName = getThumbnailName(file);
                    IOUtils.copy(is, new FileOutputStream(thumbnailName));
                    result = new VOFile(thumbnailName);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return result;
    }

    private VOFile generateCoverByPdf(final List<VOFile> files) {
        VOFile voFile = getTypeFile(files, "pdf", false);
        VOFile result = null;
        try {
            if (voFile != null) {
                final File file = new File(voFile.getPath());
                PDDocument document = PDDocument.load(file);
                PDPage page = (PDPage) document.getDocumentCatalog().getAllPages().get(0);
                if (page != null) {
                    BufferedImage image = page.convertToImage();
                    result = new VOFile(getThumbnailName(file));
                    ImageIO.write(image, "jpg", new File(result.getPath()));
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return result;
    }

    private VOFile generateCoverByEpub(final List<VOFile> files) {
        VOFile voFile = getTypeFile(files, "epub", false);
        VOFile result = null;
        try {
            if (voFile != null) {
                File file = new File(voFile.getPath());
                ZipFile zip = new ZipFile(file);

                ZipEntry entry = zip.getEntry("cover.jpg");
                if (entry == null) {
                    entry = zip.getEntry("cover.jpeg");
                }
                if (entry == null) {
                    entry = zip.getEntry("images/image1.jpeg");
                }
                if (entry == null) {
                    entry = zip.getEntry("images/image1.jpg");
                }

                if (entry != null) {
                    InputStream is = zip.getInputStream(entry);

                    String thumbnailName = getThumbnailName(file);
                    IOUtils.copy(is, new FileOutputStream(thumbnailName));
                    result = new VOFile(thumbnailName);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return result;
    }

    private VOFile generateCoverByDocx(final List<VOFile> files) {
        VOFile voFile = getTypeFile(files, "docx", false);
        VOFile result = null;
        try {
            if (voFile != null) {
                File file = new File(voFile.getPath());
                ZipFile zip = new ZipFile(file);

                //                zip.entries().each { entry ->
                //                   println(entry.getName())
                //                }

                ZipEntry entry = zip.getEntry("word/media/image1.jpg");
                if (entry == null) {
                    entry = zip.getEntry("word/media/image1.jpeg");
                }

                if (entry != null) {
                    InputStream is = zip.getInputStream(entry);

                    String thumbnailName = getThumbnailName(file);
                    IOUtils.copy(is, new FileOutputStream(thumbnailName));
                    result = new VOFile(thumbnailName);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return result;
    }

    private static String getThumbnailName(File file) {
        return file.getParent() + File.separator + FilenameUtils.getBaseName(file.getAbsolutePath()) + ".jpg";
    }
}
