package bibliotheca.service

import bibliotheca.model.VOFile
import bibliotheca.model.VOFileDetail
import bibliotheca.tools.Tools
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.pegdown.PegDownProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
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

    @Autowired
    DataBaseKnihService dataBaseKnihService;

    @Autowired
    private FtMetaService ftMetaService;

    /**
     * Create object with detail file description.
     * List of files in path will be loaded internally.
     *
     * @param path path for load
     * @param key  name of book files without suffix
     * @return book description
     */
    public VOFileDetail getVoFileDetail(String path, String key) {
        File file = new File(path);

        final Collection<File> fileCollection = FileUtils.listFilesAndDirs(file, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        final List<VOFile> voFileList = fileCollection.stream()
                .filter({it.isFile()})
                .filter({object -> StringUtils.startsWith(object.getName(), key)})
                .sorted({ o1, o2 -> o1.getName().compareToIgnoreCase(o2.getName())} as Comparator<? super File>)
//                .map(f -> new VOPath(f.getName(), f.getAbsolutePath()))
                .map({p -> new VOFile(FilenameUtils.getExtension(p.getPath()), p.getName(), p.getPath())})
                .collect(Collectors.toList());

        return getVoFileDetail(path, key, voFileList);
    }

    /**
     * Create object with detail file description
     *
     * @param path       path for load
     * @param key        name of book files without suffix
     * @param voFileList list of files loaded from path (usualy loaded once for all books in path)
     * @return book description
     */
    VOFileDetail getVoFileDetail(String path, String key, List<VOFile> voFileList) {
        final String uuid = uuidService.getUuid(path, key);

        final VOFile voFile = fileService.getCover(voFileList);
        String cover = "";
        //noinspection ConstantConditions

        String nocovername = Paths.get(
                FilenameUtils.getFullPathNoEndSeparator(voFileList.get(0).getPath()), key + "." + Tools.NOCOVER
        ).toString();

        final Path noCoverPath = Paths.get(nocovername);
        if (voFile != null) {
            cover = voFile.getPath();
            if (Files.exists(noCoverPath)) {
                Files.delete(noCoverPath);
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

        final VOFileDetail fileDetail = new VOFileDetail(uuid, key, cover, getDesc(voFileList), path,
                Tools.getStringStringMap(path, FilenameUtils.getBaseName(voFileList.get(0).getName())));

        //noinspection unchecked
        if (StringUtils.isNotBlank(fileDetail.getDbknihUrl())) {
            if (StringUtils.isBlank(fileDetail.getDesc())) {
                String description = dataBaseKnihService.getDBKnihDescription(fileDetail.getDbknihUrl());
                Tools.writeDescription(fileDetail.getPath(), fileDetail.getBookFileName(), description);
                fileDetail.setDesc(description);
            }

            if (StringUtils.isBlank(fileDetail.getNazevMeta())) {
                fileDetail.setTitle(dataBaseKnihService.getTitle(fileDetail.getDbknihUrl()));
                fileDetail.setSerie(dataBaseKnihService.getSerie(fileDetail.getDbknihUrl()));
            }

            //noinspection unchecked
            if (CollectionUtils.isEmpty(fileDetail.getAuthors())) {
                fileDetail.replaceAuthors(dataBaseKnihService.getAuthors(fileDetail.getDbknihUrl()));
            }

            if (StringUtils.isBlank(fileDetail.getHodnoceniDbPocet()) || StringUtils.isBlank(fileDetail.getHodnoceniDbProcento())) {
                fileDetail.setHodnoceniDbPocet(dataBaseKnihService.getHodnoceniDbPocet(fileDetail.getDbknihUrl()));
                fileDetail.setHodnoceniDbProcento(dataBaseKnihService.getHodnoceniDbProcento(fileDetail.getDbknihUrl()));
            }

            // automatic load cover if missing
            if (StringUtils.isBlank(fileDetail.getCover())) {
                Elements elements;
                String frmCover = null;

                elements = dataBaseKnihService.getDocument(fileDetail.getDbknihUrl()).select("img.kniha_img"); // obal knihy
                for (Element element : elements) {
                    frmCover = element.attr("src");
                }

                if (StringUtils.isNoneBlank(frmCover)) {
                    String baseFileName = Paths.get(new File(path).getAbsolutePath(), fileDetail.getBookFileName()).toString();
                    VOFile coverDb = Tools.downloadCover(baseFileName, frmCover);
                    fileDetail.setCover(coverDb.getPath());
                }
            }

            if (fileDetail.isDirty()) {
                writeMetaData(path, fileDetail.getBookFileName(), fileDetail.getMetadata());
                ftMetaService.put(fileDetail); // TODO Lebeda - batch reindex
            }
        }

        final String name = new File(path).getName();
        if (!key.startsWith(name)) {
            fileDetail.setTidyUp(true);
        }

        // odkaz na autora
        final String[] split = StringUtils.split(key, "-", 2);
        if (split.length > 1) {
            String author = StringUtils.trim(split[0]);
            final String fileAbsolutePath = new File(path).getAbsolutePath();
            if (!fileAbsolutePath.endsWith(author)) {
                final File tgtPath = new File(getTgtPathByAuthor(author));
                if (tgtPath.exists()) {
                    fileDetail.setTargetPath(tgtPath.getAbsolutePath());
                    fileDetail.setAuthor(author);
                }
            }
        }

        fileDetail.getFiles().addAll(
                CollectionUtils.select(voFileList, { object ->
                    !("jpg".equalsIgnoreCase(object.getExt())
                                || Tools.NOCOVER.equalsIgnoreCase(object.getExt())
                                || "mkd".equalsIgnoreCase(object.getExt())
                                || "mht".equalsIgnoreCase(object.getExt())
                                || "yaml".equalsIgnoreCase(object.getExt())
                                || "uuid".equalsIgnoreCase(object.getExt())
                                || "mhtml".equalsIgnoreCase(object.getExt())
                                || "htmlz".equalsIgnoreCase(object.getExt())
                        )
                })
        );

        // !suf -> suff, epub, fb2, mobi
        ["epub", "fb2", "mobi"].forEach { s ->
            if (fileService.getTypeFile(voFileList, s, false) == null) {
                fileDetail.getTargets().add(s);
            }
        }

        fileDetail.getDevices().addAll(configService.getConfig().getDevices());
        return fileDetail;
    }

    private String getDesc(final List<VOFile> files) {
        VOFile readme = fileService.getTypeFile(files, "mkd", false);
        final String html;
        if (readme != null) {
            final PegDownProcessor pdp = new PegDownProcessor();
            final File file = new File(readme.getPath());

            final FileInputStream input = new FileInputStream(file);
            final List<String> strings = IOUtils.readLines(input);
            final String join = StringUtils.join(strings, "\n");
            html = pdp.markdownToHtml(join);
        } else {
            html = null;
        }

        return html;
    }

    /**
     * Create target path for fiction books.
     * For base of path use "fictionArchive" from config.
     *
     * @param author author in schema "surrname, firstname [middlenames...]"
     * @return path for move book
     */
    String getTgtPathByAuthor(String author) {
        String firstLetter = StringUtils.substring(author, 0, 1).toUpperCase();
        File beleTgt = new File(configService.getConfig().getFictionArchive());
        return Paths.get(beleTgt.getAbsolutePath(), firstLetter, author).toString();
    }


    void writeMetaData(String path, String basename, Map<String, Object> metadata) {
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);

        Yaml yaml = new Yaml(options);
        Writer writer = new FileWriter(Paths.get(path, basename + ".yaml").toFile());
        yaml.dump(metadata, writer);
    }
}
