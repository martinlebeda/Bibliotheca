package bibliotheca.web;

import bibliotheca.model.VOFile;
import bibliotheca.model.VOFileDetail;
import bibliotheca.model.VOUuid;
import bibliotheca.service.*;
import bibliotheca.tools.Tools;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 8.9.15
 */

@RestController
public class MainRestControler {

    @Autowired
    FileService fileService;

    @Autowired
    BrowsePageService browsePageService;

    @Autowired
    UuidService uuidService;

    @Autowired
    BookDetailService bookDetailService;

    @Autowired
    DataBaseKnihService dataBaseKnihService;


    @RequestMapping("/cover")
    public byte[] cover(@RequestParam("path") String path) {
        File file = new File(path);
        try {
            BufferedImage image = ImageIO.read(file);
            BufferedImage thumbnail = Scalr.resize(image, Tools.THUMBNAIL_SIZE);
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "jpg", output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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
            try {
                ZipFile zip = new ZipFile(baseFile);
                ZipEntry entry = zip.getEntry(fileName);
                if (entry != null) {
                    InputStream is = zip.getInputStream(entry);
                    IOUtils.copy(is, output);
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        return output.toByteArray();
    }

    @RequestMapping("/download")
    public
    @ResponseBody
    void download(@RequestParam("path") String path, HttpServletResponse response) {
        try {
            File file = new File(path);

            response.setContentType("application/octet-stream");
            response.setContentLength((new Long(file.length()).intValue()));
            response.setHeader("content-Disposition", "attachment; filename=\"" + StringUtils.stripAccents(file.getName()) + "\"");// "attachment;filename=test.xls"
            IOUtils.copyLarge(new FileInputStream(file), response.getOutputStream());

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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

        VOFileDetail fd = bookDetailService.getVoFileDetail(path, name);
        List<VOFile> files = fd.getFiles();
        files.stream().forEach(voFile -> {
            File file = new File(voFile.getPath());
            if (file.exists()) {
                file.delete();
            }
        });

        dataBaseKnihService.clearMetadata(path, name);

        File uuidFile = Paths.get(path, name + ".uuid").toFile();
        if (uuidFile.exists()) {
            uuidFile.delete();
        }

        uuidService.removeFromCache(id);
    }

    // TODO - JavaDoc - Lebeda
    @RequestMapping("/tidyupBook")
    public void tidyupBook(@RequestParam("id") String id) {
        VOUuid voUuid = uuidService.getByUuid(id);

        final File[] listFiles = new File(voUuid.getPath()).listFiles((dir, jm) -> jm.startsWith(voUuid.getName()));

        Arrays.stream(listFiles).forEach((fileUklid) -> {
            final String[] split = StringUtils.split(fileUklid.getName(), "-", 2);
            String author = StringUtils.trim(split[0]);
            File tgt = new File(bookDetailService.getTgtPathByAuthor(author));
            fileService.tidyUp(fileUklid, Paths.get(tgt.getAbsolutePath(), fileUklid.getName()).toFile());
        });

        uuidService.removeFromCache(id);
    }

    @RequestMapping("/joinTo")
    public void joinTo(@RequestParam("idFrom") String idFrom, @RequestParam("idTo") String idTo) {
        VOUuid voUuidFrom = uuidService.getByUuid(idFrom);
        VOUuid voUuidTo = uuidService.getByUuid(idTo);

        final File[] listFiles = new File(voUuidFrom.getPath()).listFiles((dir, jm) -> jm.startsWith(voUuidFrom.getName()));

        Arrays.stream(listFiles).forEach((fileFrom) -> {
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

        uuidService.removeFromCache(voUuidFrom.getUuid());
    }

}
