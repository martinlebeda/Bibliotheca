package bibliotheca.web;

import bibliotheca.model.VOFile;
import bibliotheca.service.BrowsePageService;
import bibliotheca.service.FileService;
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
import java.util.ArrayList;
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

    @RequestMapping("/view/**")
    public byte[] view(HttpServletRequest request) {
        String restOfTheUrl = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String splat = restOfTheUrl.replaceFirst("/view/", "");

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
    public @ResponseBody void download(@RequestParam("path") String path, HttpServletResponse response) {
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

}
