package bibliotheca.service.impl;

import bibliotheca.config.ConfigService;
import bibliotheca.model.VOFile;
import bibliotheca.model.VOPath;
import bibliotheca.service.BookDetailService;
import bibliotheca.service.FileService;
import bibliotheca.service.UuidService;
import bibliotheca.tools.Tools;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 16.12.14
 */
@Service
public class FileServiceImpl implements FileService {


//    protected final Request request;
//    protected final Response response;

    public static final String FRM_NAME = "bookname";
    public static final String FRM_COVER = "bookcover";
    public static final String FRM_DESCRIPTION = "bookdescription";

    public static final String TIDYUP = "tidyup";

    @Autowired
    private UuidService uuidService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private BookDetailService bookDetailService;

    @Override
    public void fillNavigatorData(final HashMap<String, Object> model,
                                  final File file,
                                  final boolean navigableLastFile) {
        final ArrayList<VOPath> navigator = new ArrayList<>();
        if (!CollectionUtils.exists(configService.getConfig().getFictionPaths(), s -> s.equalsIgnoreCase(file.getAbsolutePath()))) {
            for (File navigatorFile : getNavigatorData(file.getParentFile())) {
                navigator.add(new VOPath(navigatorFile.getName(), navigatorFile.getAbsolutePath()));
            }
        }
        model.put("navigator", navigator);
        model.put("navigatorFile", FilenameUtils.getBaseName(file.getName()));
        model.put("navigatorPath", file.getAbsolutePath());
        model.put("navigableLastFile", navigableLastFile);
    }

    @Override
    public File[] refreshFiles(final String basename, final File file, List<VOFile> voFileList) {
        final File[] listFiles;
        listFiles = file.listFiles((dir, name1) -> name1.startsWith(FilenameUtils.getBaseName(basename)));
        voFileList.clear();
        Arrays.stream(listFiles).forEach(file1 -> voFileList.add(new VOFile(file1.getAbsolutePath())));
        return listFiles;
    }

    @Override
    public VOFile getTypeFile(List<VOFile> files, final String suffix, final boolean generate) {
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

    @SneakyThrows
    private void generateFile(final List<VOFile> files, final String suffix) {
        VOFile srcFile = getTypeFile(files, "htmlz", false);
        if (srcFile == null) {
            srcFile = getTypeFile(files, "epub", false);
        }
        if (srcFile == null) {
            srcFile = getTypeFile(files, "mobi", false);
        }
        if (srcFile == null) {
            srcFile = getTypeFile(files, "azw3", false);
        }
        if (srcFile == null) {
            srcFile = getTypeFile(files, "azw", false);
        }
        if (srcFile == null) {
            srcFile = getTypeFile(files, "rtf", false);
        }
        if (srcFile == null) {
            srcFile = getTypeFile(files, "odt", false);
        }
        if (srcFile == null) {
            srcFile = getTypeFile(files, "docx", false);
        }
        if (srcFile == null) {
            srcFile = getTypeFile(files, "prc", false);
        }
        if (srcFile == null) {
            srcFile = getTypeFile(files, "pdb", false);
        }
        if (srcFile == null) {
            srcFile = getTypeFile(files, "txt", false);
        }

        // podpota pro doc
        if (srcFile == null) {
            srcFile = getDocxFromDocFile(files);
        }


        if (srcFile != null) {
            // generate
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
            command.add(configService.getConfig().getConvert());
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
        }
    }

    @SneakyThrows
    private VOFile getDocxFromDocFile(final List<VOFile> files) {
        VOFile srcFile = null;
        final VOFile doc = getTypeFile(files, "doc", false);
        if (doc != null) {
            final List<String> command = new ArrayList<>();
            command.add(configService.getConfig().getLibreoffice());
            command.add("--headless");
            command.add("--convert-to");
            command.add("docx");
            command.add(doc.getPath());

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(new File(doc.getPath()).getParentFile());
            final Process process;
            process = builder.start();
            process.waitFor();
        }

        // refresh
        if (CollectionUtils.isNotEmpty(files)) {
            final VOFile voFile = files.get(0);
            if (voFile != null) {
                refreshFiles(voFile.getName(), new File(voFile.getPath()).getParentFile(), files);
                srcFile = getTypeFile(files, "docx", false);
            }
        }
        return srcFile;
    }

    private List<File> getNavigatorData(File file) {
        List<File> parentFiles = new ArrayList<>();
        String parentName = file.getAbsolutePath();
        while (parentName.length() > 1) {
            final String finalParentName = parentName;
            Boolean end = CollectionUtils.exists(configService.getConfig().getFictionPaths(), s -> s.equalsIgnoreCase(finalParentName));
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

    @Override
    public VOFile getCover(final List<VOFile> files) {
        VOFile cover = getTypeFile(files, "jpg", false);

        if (getTypeFile(files, Tools.NOCOVER, false) == null) {
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

                result = getVoFile(result, file, zip, entry);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return result;
    }

    private static VOFile getVoFile(VOFile result, File file, ZipFile zip, ZipEntry entry) throws IOException {
        if (entry != null) {
            InputStream is = zip.getInputStream(entry);

            String thumbnailName = Tools.getThumbnailName(file);
            IOUtils.copy(is, new FileOutputStream(thumbnailName));
            result = new VOFile(thumbnailName);
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
                    result = new VOFile(Tools.getThumbnailName(file));
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

                result = getVoFile(result, file, zip, entry);
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

                result = getVoFile(result, file, zip, entry);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return result;
    }

    @Override
    @SneakyThrows
    public void tidyUp(final File fileFrom, final File tgtFile) {
        if (tgtFile.exists()) {
            String sha1Src = DigestUtils.sha1Hex(new FileInputStream(fileFrom));
            String sha1Tgt = DigestUtils.sha1Hex(new FileInputStream(tgtFile));

            if ((sha1Src.equals(sha1Tgt)
                    || fileFrom.getName().endsWith("uuid"))
                    || fileFrom.getName().endsWith("yaml")
                    || fileFrom.getName().endsWith("mkd")
                    || fileFrom.getName().endsWith("jpg")
                    || fileFrom.getName().endsWith("yaml")
                    ) {
                //noinspection ResultOfMethodCallIgnored
                fileFrom.delete();
                if (fileFrom.getName().endsWith("uuid")) {
                    final String id = uuidService.getUuid(fileFrom.getParent(), FilenameUtils.getBaseName(fileFrom.getName()));
                    uuidService.removeFromCache(id);
                }
            } else {
                String pattern = "yyyyMMdd";
                SimpleDateFormat format = new SimpleDateFormat(pattern);
                String newFileName = FilenameUtils.getBaseName(fileFrom.getName()) + ".bak"
                        + format.format(new Date(fileFrom.lastModified()))
                        + "." + FilenameUtils.getExtension(fileFrom.getName());
                Path tgtFileNahr = Paths.get(tgtFile.getParent(), newFileName);
                FileUtils.moveFile(fileFrom, tgtFileNahr.toFile());
            }
        } else {
            FileUtils.moveFile(fileFrom, tgtFile);
            if (fileFrom.getName().endsWith("uuid")) {
                final String id = uuidService.getUuid(fileFrom.getParent(), FilenameUtils.getBaseName(fileFrom.getName()));
                uuidService.removeFromCache(id);
                uuidService.getUuid(tgtFile.getParent(), FilenameUtils.getBaseName(tgtFile.getName()));
            }
        }
    }

    @Override
    public void tidyUpBook(String name, String path) {
        final File[] listFiles = new File(path).listFiles((dir, jm) -> {
            return jm.startsWith(name);
        });

        Arrays.stream(listFiles).forEach((fileUklid) -> {
            final String[] split = StringUtils.split(fileUklid.getName(), "-", 2);
            String author = StringUtils.trim(split[0]);
            File tgt = new File(bookDetailService.getTgtPathByAuthor(author));
            tidyUp(fileUklid, Paths.get(tgt.getAbsolutePath(), fileUklid.getName()).toFile());
        });
    }

}
