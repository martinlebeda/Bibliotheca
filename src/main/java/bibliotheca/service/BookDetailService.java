package bibliotheca.service;

import bibliotheca.config.ConfigService;
import bibliotheca.model.VOFile;
import bibliotheca.model.VOFileDetail;
import bibliotheca.model.VOPath;
import bibliotheca.tools.Tools;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.pegdown.PegDownProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 3.12.15
 */
@Service
public class BookDetailService {

    @Autowired
    private FileService fileService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private UuidService uuidService;

    public VOFileDetail getVoFileDetail(String path, String key) {
        File file = new File(path);

        final Collection<File> fileCollection = FileUtils.listFilesAndDirs(file, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        final List<VOFile> voFileList = fileCollection.stream()
                .filter(File::isFile)
                .filter(object -> StringUtils.startsWith(object.getName(), key))
                .sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()))
                .map(f -> new VOPath(f.getName(), f.getAbsolutePath()))
                .map(p -> new VOFile(FilenameUtils.getExtension(p.getPath()), p.getName(), p.getPath()))
                .collect(Collectors.toList());

        return getVoFileDetail(path, key, voFileList);
    }

    public VOFileDetail getVoFileDetail(String path, String key, List<VOFile> voFileList) {
        File file = new File(path);

        final String uuid = uuidService.getUuid(path, key);

        final VOFile voFile = fileService.getCover(voFileList);
        String cover = "";
        //noinspection ConstantConditions
        String nocovername = Paths.get(
                FilenameUtils.getFullPathNoEndSeparator(voFileList.get(0).getPath()),
                key + "." + Tools.NOCOVER
        ).toString();
        final Path noCoverPath = Paths.get(nocovername);
        if (voFile != null) {
            cover = voFile.getPath();
            if (Files.exists(noCoverPath)) {
                try {
                    Files.delete(noCoverPath);
                } catch (IOException e) {
                    // nothing
                }
            }
        } else {
            if (!Files.exists(noCoverPath)) {
                try {
                    FileOutputStream fos = new FileOutputStream(nocovername);
                    fos.write("no cover".getBytes());
                    fos.close();
                } catch (IOException e) {
                    // nothing
                }
            }
        }

        final String desc = getDesc(voFileList);

        Map<String, Object> metadata = Tools.getStringStringMap(path, FilenameUtils.getBaseName(voFileList.get(0).getName()));

        @SuppressWarnings("unchecked") final VOFileDetail fileDetail = new VOFileDetail(uuid, key, cover, desc,
                (String) metadata.get(Tools.METADATA_KEY_DATABAZEKNIH_CZ),
                (String) metadata.get(Tools.METADATA_KEY_NAZEV),
                (String) metadata.get(Tools.METADATA_KEY_SERIE),
                (List<String>) metadata.get(Tools.METADATA_KEY_AUTHORS)
        );

        //noinspection unchecked
        if (StringUtils.isNotBlank(fileDetail.getDbknihUrl())
                && (StringUtils.isBlank((String) metadata.get(Tools.METADATA_KEY_NAZEV))
                || CollectionUtils.isEmpty((List<String>) metadata.get(Tools.METADATA_KEY_AUTHORS)))
                ) {
            try {
                Document doc = Jsoup.connect((String) metadata.get(Tools.METADATA_KEY_DATABAZEKNIH_CZ)).timeout(Tools.CONNECT_TIMEOUT_MILLIS).get();
                if (loadFromDBKnih(metadata, fileDetail, doc)) {
                    Tools.writeMetaData(path, fileDetail.getName(), metadata);
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        // automatic load cover if missing
        if (StringUtils.isNotBlank(fileDetail.getDbknihUrl())
                && !fileDetail.getCoverExists()) {
            Document doc;
            try {
                doc = Jsoup.connect((String) metadata.get(Tools.METADATA_KEY_DATABAZEKNIH_CZ)).timeout(Tools.CONNECT_TIMEOUT_MILLIS).get();

                Elements elements;
                String frmCover = null;


                elements = doc.select("img.kniha_img"); // obal knihy
                for (Element element : elements) {
                    frmCover = element.attr("src");
                }

                if (StringUtils.isNoneBlank(frmCover)) {
                    String baseFileName = Paths.get(file.getAbsolutePath(), fileDetail.getName()).toString();
                    VOFile coverDb = Tools.downloadCover(baseFileName, frmCover);
                    fileDetail.setCover(coverDb.getPath());
                }


            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        final String name = file.getName();
        if (!key.startsWith(name)) {
            fileDetail.setTidyUp(true);
        }

        // odkaz na autora
        final String[] split = StringUtils.split(key, "-", 2);
        if (split.length > 1) {
            String author = StringUtils.trim(split[0]);
            final String fileAbsolutePath = file.getAbsolutePath();
            if (!fileAbsolutePath.endsWith(author)) {
                final File tgtPath = new File(getTgtPathByAuthor(author));
                if (tgtPath.exists()) {
                    fileDetail.setTargetPath(tgtPath.getAbsolutePath());
                    fileDetail.setAuthor(author);
                }
            }
        }

        fileDetail.getFiles().addAll(
                CollectionUtils.select(voFileList,
                        object -> !("jpg".equalsIgnoreCase(object.getExt())
                                || Tools.NOCOVER.equalsIgnoreCase(object.getExt())
                                || "mkd".equalsIgnoreCase(object.getExt())
                                || "mht".equalsIgnoreCase(object.getExt())
                                || "yaml".equalsIgnoreCase(object.getExt())
                                || "uuid".equalsIgnoreCase(object.getExt())
                                || "mhtml".equalsIgnoreCase(object.getExt())
                                || "htmlz".equalsIgnoreCase(object.getExt())
                        )
                )
        );

        // !suf -> suff, epub, fb2, mobi
        Arrays.stream(new String[]{"epub", "fb2", "mobi"}).forEach(s -> {
            if (fileService.getTypeFile(voFileList, s, false) == null) {
                fileDetail.getTargets().add(s);
            }
        });

        fileDetail.getDevices().addAll(configService.getConfig().getDevices());
        return fileDetail;
    }

    private String getDesc(final List<VOFile> files) {
        VOFile readme = fileService.getTypeFile(files, "mkd", false);
        final String html;
        if (readme != null) {
            final PegDownProcessor pdp = new PegDownProcessor();
            final File file = new File(readme.getPath());
            try {
                final FileInputStream input = new FileInputStream(file);
                final List<String> strings = IOUtils.readLines(input);
                final String join = StringUtils.join(strings, "\n");
                html = pdp.markdownToHtml(join);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            html = null;
        }

        return html;
    }

    String getTgtPathByAuthor(String author) {
        String firstLetter = StringUtils.substring(author, 0, 1).toUpperCase();
        File beleTgt = new File(configService.getConfig().getFictionArchive());
        return Paths.get(beleTgt.getAbsolutePath(), firstLetter, author).toString();
    }

    boolean loadFromDBKnih(Map<String, Object> metadata, VOFileDetail fileDetail, Document doc) {
        boolean saveChange = false;

        String nazev = Tools.getDBKnihNazev(doc);
        if (StringUtils.isNotBlank(nazev)) {
            if (fileDetail != null) {
                fileDetail.setNazev(nazev);
            }
            metadata.put(Tools.METADATA_KEY_NAZEV, nazev);
            saveChange = true;
        }

        String serie = Tools.getDBKnihSerie(doc);
        if (StringUtils.isNotBlank(serie)) {
            if (fileDetail != null) {
                fileDetail.setSerie(serie);
            }
            metadata.put(Tools.METADATA_KEY_SERIE, serie);
            saveChange = true;
        }

        List<String> authors = Tools.getDBKnihAuthors(doc);
        if (CollectionUtils.isNotEmpty(authors)) {
            if (fileDetail != null) {
                fileDetail.getAuthors().clear();
                fileDetail.getAuthors().addAll(authors);
            }
            metadata.put(Tools.METADATA_KEY_AUTHORS, authors);
            saveChange = true;
        }
        return saveChange;
    }
}
