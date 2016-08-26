package bibliotheca.service

import bibliotheca.model.VOFile
import bibliotheca.tools.Tools
import lombok.SneakyThrows
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import java.nio.file.Paths
import java.util.stream.Collectors
/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 17.12.14
 */
@Service
public class EditFilePageService {

    @Autowired
    private FileService fileService;

    @Autowired
    private DataBaseKnihService dataBaseKnihService;

    @Autowired
    private BookDetailService bookDetailService;


    public Map<String, Object> getModel(String path, String basename, final String frmName, String frmCover, String frmDescription, String dbknih,
                                        String loadImage, String loadDescription, String loadAll, String loadAllClose, String tryDbKnih) {
        final HashMap<String, Object> model = Tools.getDefaultModel("Bibliotheca - edit file", path);

        File file = new File(path);
        model.put(Tools.PARAM_PATH, file.getAbsolutePath());


        List<VOFile> voFileList = new ArrayList<>();
        final String finalBasename2 = basename;
        File[] listFiles = file.listFiles({ pathname ->
            return pathname.getName().startsWith(finalBasename2);
        });
        Arrays.stream(listFiles).forEach({ file1 -> voFileList.add(new VOFile(file1.getAbsolutePath())) });

        // rename if need
        if (StringUtils.isNoneBlank(frmName) && !basename.equals(frmName)) {
            Tools.renameAll(basename, listFiles, frmName);

            // refresh names
            basename = frmName;
            listFiles = fileService.refreshFiles(basename, file, voFileList);
        }

        String bookname = StringUtils.replacePattern(basename, ".*- *", "");
        model.put("bookname", bookname);

        // try search automatic in databaze knih
        if (StringUtils.isNotBlank(tryDbKnih)) {
            dbknih = dataBaseKnihService.getAutomaticDBKnihUrl(bookname);
            loadDescription = dbknih;
        }

        // read metadata
        Map<String, Object> metadata = Tools.getStringStringMap(path, basename);

        // set matadata
        if (StringUtils.isNotBlank(dbknih) && !dbknih.equals(metadata.get(Tools.METADATA_KEY_DATABAZEKNIH_CZ))) {
            metadata.put(Tools.METADATA_KEY_DATABAZEKNIH_CZ, dbknih);
            bookDetailService.writeMetaData(path, basename, metadata);
        }

        // save metadata
        model.putAll(metadata);


        // load and fill from DBKnih
        String url = (String) metadata.get(Tools.METADATA_KEY_DATABAZEKNIH_CZ);
        if ((StringUtils.isNotBlank(loadImage)
                || StringUtils.isNotBlank(loadDescription)
                || StringUtils.isNotBlank(loadAll)
                || StringUtils.isNotBlank(loadAllClose)
        ) && StringUtils.isNotBlank(url)) {
            Document doc = dataBaseKnihService.getDocument(url);

            Elements elements;

            if (StringUtils.isNotBlank(loadImage)
                    || StringUtils.isNotBlank(loadAll)
                    || StringUtils.isNotBlank(loadAllClose)) {
                elements = doc.select("img.kniha_img"); // obal knihy
                for (Element element : elements) {
                    frmCover = element.attr("src");
                }
            }

            if (StringUtils.isNotBlank(loadDescription)
                    || StringUtils.isNotBlank(loadAll)
                    || StringUtils.isNotBlank(loadAllClose)) {
                frmDescription = dataBaseKnihService.getDBKnihDescription(url);
            }
        }

        // download new cower if need
        VOFile cover = fileService.getCover(voFileList);
        String baseFileName = Paths.get(file.getAbsolutePath(), basename).toString();
        if (StringUtils.isNoneBlank(frmCover)) {
            cover = Tools.downloadCover(baseFileName, frmCover);
        }

        // create description if need
        String desc = getRawDesc(voFileList);
        if (StringUtils.isNoneBlank(frmDescription) && (!frmDescription.equals(desc))) {
            desc = Tools.writeDescription(path, basename, frmDescription);
        }

        fileService.fillNavigatorData(model, listFiles[0], false);

        model.put("name", basename);
        model.put("author", file.getName());
        model.put("optnames", Arrays.stream(file.listFiles({ it.isFile() }))
                        .map({ file1 -> FilenameUtils.getBaseName(file1.getName()) })
                        .collect(Collectors.toSet())
        );

        model.put("desc", desc);
        model.put("cover", (cover != null ? cover.getPath() : null));

        return model;
    }

    public void saveDbUrl(String path, String name, String url) {
        // read metadata
        Map<String, Object> metadata = Tools.getStringStringMap(path, name);

        // set matadata
        if (StringUtils.isNotBlank(url) && !url.equals(metadata.get(Tools.METADATA_KEY_DATABAZEKNIH_CZ))) {
            metadata.put(Tools.METADATA_KEY_DATABAZEKNIH_CZ, url);

            bookDetailService.writeMetaData(path, name, metadata);
        }
    }

    @SneakyThrows
    private String getRawDesc(final List<VOFile> files) {
        VOFile readme = fileService.getTypeFile(files, "mkd", false);
        final String html;
        if (readme != null) {
            final File file = new File(readme.getPath());
            final FileInputStream input = new FileInputStream(file);
            final List<String> strings = IOUtils.readLines(input);
            html = StringUtils.join(strings, "\n");
        } else {
            html = null;
        }

        return html;
    }

}
