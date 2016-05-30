package bibliotheca.web

import bibliotheca.model.VOFile
import bibliotheca.model.VOUuid
import bibliotheca.service.*
import bibliotheca.tools.Tools
import groovy.util.logging.Log
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.imgscalr.Scalr
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.HandlerMapping

import javax.imageio.ImageIO
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.awt.image.BufferedImage
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 8.9.15
 */

@Log
@RestController
public class MainRestControler {

    @Autowired
    private FileService fileService;

    @Autowired
    private BrowsePageService browsePageService;

    @Autowired
    private UuidService uuidService;

    @Autowired
    private BookDetailService bookDetailService;

    @Autowired
    private DataBaseKnihService dataBaseKnihService;

    @Autowired
    private FtMetaService ftMetaService;


    @RequestMapping("/cover")
    public byte[] cover(@RequestParam("path") String path) {
        File file = new File(path);
        BufferedImage image = ImageIO.read(file);
        BufferedImage thumbnail = Scalr.resize(image, Tools.THUMBNAIL_SIZE);
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, "jpg", output);
        return output.toByteArray();
    }

    //    @RequestMapping("/view/**{variable:.+}")
//    public byte[] view(@PathVariable String variable, HttpServletRequest request) {
    @RequestMapping("/view/**")
    public byte[] view(HttpServletRequest request) {
        String restOfTheUrl = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String splat = restOfTheUrl.replaceFirst("/view/", "").replaceAll("\\+", " ");

        final String[] strings = StringUtils.splitByWholeSeparator(splat, "/pack/");
        final String baseName = File.separator + strings[0];
        final String fileName = strings[1];

        File baseFile = new File(baseName);

        final List<VOFile> files = new ArrayList<>();
        fileService.refreshFiles(FilenameUtils.getBaseName(baseFile.getName()), baseFile.getParentFile(), files);
        VOFile voFile = fileService.getTypeFile(files, "htmlz", true);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (voFile != null) {
            ZipFile zip = new ZipFile(baseFile);
            ZipEntry entry = zip.getEntry(fileName);
            if (entry != null) {
                InputStream is = zip.getInputStream(entry);
                IOUtils.copy(is, output);
            }
        }

        return output.toByteArray();
    }

    @RequestMapping("/download")
    public @ResponseBody void download(@RequestParam("path") String path, HttpServletResponse response) {
        File file = new File(path);

        response.setContentType("application/octet-stream");
        response.setContentLength((new Long(file.length()).intValue()));
        response.setHeader("content-Disposition", "attachment; filename=\"" + StringUtils.stripAccents(file.getName()) + "\"");// "attachment;filename=test.xls"
        IOUtils.copyLarge(new FileInputStream(file), response.getOutputStream());
    }

    /**
     * delete all files of book from disk
     *
     * @param id id of book
     */
    @RequestMapping("/deleteBook")
    public void deleteBook(@RequestParam("id") String id) {
        VOUuid voUuid = uuidService.getByUuid(id);
        String path = voUuid.getPath();
        String name = voUuid.getName();

        Paths.get(path).toFile().eachFile {
            if (it.name.startsWith(name)) {
                log.info("deleting - ${it.name}")
                it.delete()
            }
        }

        uuidService.removeFromCache(id);
        ftMetaService.remove(id);
    }

    // TODO - JavaDoc - Lebeda
    @RequestMapping("/tidyupBook")
    public void tidyupBook(@RequestParam("id") String id) {
        VOUuid voUuid = uuidService.getByUuid(id);
        final String name = voUuid.getName();
        final String path = voUuid.getPath();

        fileService.tidyUpBook(name, path);
    }

    @RequestMapping("/refreshIndex")
    public void refreshIndex() {
        uuidService.refreshUuids();
        ftMetaService.reindexAll();
    }

    @RequestMapping("/joinTo")
    public void joinTo(@RequestParam("idFrom") String idFrom, @RequestParam("idTo") String idTo) {
        VOUuid voUuidFrom = uuidService.getByUuid(idFrom);
        VOUuid voUuidTo = uuidService.getByUuid(idTo);

        final File[] listFiles = new File(voUuidFrom.getPath().trim()).listFiles({ File f ->
            def name = voUuidFrom.getName()
            f.name.startsWith(name) } as FileFilter);

        Arrays.stream(listFiles).forEach({ fileFrom ->
            if (fileFrom.getName().endsWith("nocover") || fileFrom.getName().endsWith("uuid")) {
                fileFrom.delete();
            } else {
                File fileTo = new File(voUuidTo.getPath());
                String tgtPath = fileTo.getAbsolutePath();
                String tgtName = voUuidTo.getName();
                String tgtExt = FilenameUtils.getExtension(fileFrom.getName());

                File tgt = Paths.get(tgtPath, tgtName + "." + tgtExt).toFile();
                fileService.tidyUp(fileFrom, tgt);
            }
        });

        uuidService.removeFromCache(voUuidFrom.getUuid())
        ftMetaService.remove(voUuidFrom.uuid)
    }

    @RequestMapping("/joinToDir")
    public void joinToDir(@RequestParam("srcPath") String srcPath, @RequestParam("tgtPath") String tgtPath) {
        File srcFile = new File(srcPath);

        final File[] listFiles = srcFile.listFiles();

        Arrays.stream(listFiles)
                .sorted()
                .forEach({ file ->
                    File tgt = Paths.get(tgtPath, file.getName()).toFile();
                    fileService.tidyUp(file, tgt);
                });

        FileUtils.deleteDirectory(srcFile);
    }

    @RequestMapping("/tryDb")
        public String tryDb(@RequestParam("id") String id) {
        VOUuid voUuid = uuidService.getByUuid(id);
        final boolean tryDb = browsePageService.tryDb(voUuid.getPath(), voUuid.getName());
        if (tryDb) {
            return "{\"tryDb\": 1}";

        } else {
            return "{\"tryDb\": 0}";
        }
    }

}
