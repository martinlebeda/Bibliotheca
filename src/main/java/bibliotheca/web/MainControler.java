package bibliotheca.web;

import bibliotheca.config.ConfigService;
import bibliotheca.model.VOFile;
import bibliotheca.model.VOFileDetail;
import bibliotheca.model.VOPath;
import bibliotheca.model.VOUuid;
import bibliotheca.service.*;
import bibliotheca.tools.Tools;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 8.9.15
 */

@Controller
public class MainControler {

    @Autowired
    private ConfigService configService;

    @Autowired
    private BrowsePageService browsePageService;

    @Autowired
    private EditFilePageService editFilePageService;

    @Autowired
    private FileService fileService;

    @Autowired
    private DataBaseKnihService dataBaseKnihService;

    @Autowired
    private BookDetailService bookDetailService;

    @Autowired
    private UuidService uuidService;

    @RequestMapping("/")
    public String root(final Model model) {

        model.addAttribute("title", "Bibliotheca - bibliotheca.web.Main page");

        final List<VOPath> belePaths = configService.getConfig().getFictionPaths()
                .stream()
                .map(fpath -> new VOPath(FilenameUtils.getBaseName(fpath), fpath))
                .collect(Collectors.toList());
        model.addAttribute("bele", belePaths);

        return "MainPage";
    }

    @RequestMapping("/browse")
    public String browse(@RequestParam("path") String path,
                         @RequestParam(value = "booksearch", required = false) String booksearch,
                         @RequestParam(value = "action", required = false) String action,
                         @RequestParam(value = "basename", required = false) String basename,
                         final Model model) {
        if ("trydball".equals(action)) {
            browsePageService.tryDbAll(path);
        }
        if ("cleanupall".equals(action)) {
            browsePageService.tidyUpAll(path);
        }

        model.addAllAttributes(browsePageService.getModel(path, booksearch, basename));
        return "BrowsePage";
    }

    @RequestMapping("/tryDb")
    public String tryDb(@RequestParam("id") String id, final Model model) {
        VOUuid voUuid = uuidService.getByUuid(id);
        // TODO Lebeda - přesunout
        model.addAllAttributes(browsePageService.tryDb(voUuid.getPath(), voUuid.getName()));
        return "BrowsePage :: bookitem";
    }

    @RequestMapping("/loadItem")
    public String loadItem(@RequestParam("id") String id, final Model model) {
        VOUuid voUuid = uuidService.getByUuid(id);
        VOFileDetail fd = bookDetailService.getVoFileDetail(voUuid.getPath(), voUuid.getName());
        model.addAttribute("p", fd);
        return "BrowsePage :: bookitem";
    }

    @RequestMapping("/generateTgt")
    public String generateTgt(@RequestParam("id") String id, @RequestParam("tgt") String target, final Model model) {
        VOUuid voUuid = uuidService.getByUuid(id);

        // generate
        List<VOFile> fileList = getVoFiles(voUuid);

        if (fileService.getTypeFile(fileList, target, false) == null) {
            fileService.getTypeFile(fileList, target, true);
        }

        VOFileDetail fd = bookDetailService.getVoFileDetail(voUuid.getPath(), voUuid.getName());
        model.addAttribute("p", fd);
        return "BrowsePage :: bookitem";
    }

    @RequestMapping("/toReader")
    public String toReader(@RequestParam("id") String id,
                           @RequestParam("devFormat") String devFormat,
                           @RequestParam("devPath") String devicePath,
                           final Model model) {
        VOUuid voUuid = uuidService.getByUuid(id);

        // to device
        if (StringUtils.isNotBlank(devicePath)) {
            List<VOFile> fileList = getVoFiles(voUuid);

            final VOFile voFile = fileService.getTypeFile(fileList, devFormat, true);
            if (voFile != null) {
                try {
                    FileUtils.copyFileToDirectory(new File(voFile.getPath()), new File(devicePath));
                } catch (IOException e) {
                    throw new IllegalStateException(e);

                }
            }
        }

        VOFileDetail fd = bookDetailService.getVoFileDetail(voUuid.getPath(), voUuid.getName());
        model.addAttribute("p", fd);
        return "BrowsePage :: bookitem";
    }

    // TODO - JavaDoc - Lebeda
    private List<VOFile> getVoFiles(VOUuid voUuid) {
        final File[] listFiles = new File(voUuid.getPath()).listFiles((dir, name) -> name.startsWith(voUuid.getName()));
        return Arrays.stream(listFiles)
                .map(file1 -> new VOFile(file1.getAbsolutePath()))
                .collect(Collectors.toList());
    }

    @RequestMapping("/chooseDbModalList")
    public String chooseDbModalList(@RequestParam("id") String id, final Model model) {
        VOUuid voUuid = uuidService.getByUuid(id);
        VOFileDetail fd = bookDetailService.getVoFileDetail(voUuid.getPath(), voUuid.getName());
        model.addAttribute("chlist", dataBaseKnihService.getChooseDbModalList(fd.getBookname()));
        return "BrowsePage :: chooseListItem";
    }

    @RequestMapping("/chooseJoinModalData")
    public String chooseJoinModalData(@RequestParam("id") String id, final Model model) {
        VOUuid voUuid = uuidService.getByUuid(id);
        model.addAttribute("chlist", dataBaseKnihService.getChooseJoinModalDataList(voUuid.getPath(), voUuid.getName()));
        model.addAttribute("idFrom", id);
        return "BrowsePage :: chooseJoinModalData";
    }

    @RequestMapping("/downloadCover")
    public String downloadCover(@RequestParam("id") String id, final Model model) {
        VOUuid voUuid = uuidService.getByUuid(id);
        VOFileDetail fd = bookDetailService.getVoFileDetail(voUuid.getPath(), voUuid.getName());
        dataBaseKnihService.downloadCover(fd, true);
        model.addAttribute("p", fd);
        return "BrowsePage :: bookitem";
    }

    @RequestMapping("/clearMetadata")
    public String clearMetadata(@RequestParam("id") String id, final Model model) {
        VOUuid voUuid = uuidService.getByUuid(id);
        dataBaseKnihService.clearMetadata(voUuid.getPath(), voUuid.getName());
        VOFileDetail fd = bookDetailService.getVoFileDetail(voUuid.getPath(), voUuid.getName());
        model.addAttribute("p", fd);
        return "BrowsePage :: bookitem";
    }

    @RequestMapping(value = "/saveDbUrl")
    public String saveDbUrl(@RequestParam("id") String id, @RequestParam("url") String url, final Model model) {
        VOUuid voUuid = uuidService.getByUuid(id);

        editFilePageService.saveDbUrl(voUuid.getPath(), voUuid.getName(), url);

        VOFileDetail fd = bookDetailService.getVoFileDetail(voUuid.getPath(), voUuid.getName());
        dataBaseKnihService.loadFromDBKnih(fd, true);
        model.addAttribute("p", fd);

        model.addAllAttributes(Tools.getDefaultModel("Bibliotheca - Browse fiction", voUuid.getPath()));


        return "BrowsePage :: bookitem";
    }

    @RequestMapping("/editDir")
    public String editDir(
            @RequestParam("path") String path,
            @RequestParam("bookname") String bookname,
            final Model model) {
        final HashMap<String, Object> oldModel = Tools.getDefaultModel("Bibliotheca - Browse fiction", path);
        File file = new File(path);
        oldModel.put(Tools.PARAM_PATH, file.getAbsolutePath());

        // rename if need
        String frmName = bookname;
        if (StringUtils.isNoneBlank(frmName) && !file.getName().equals(frmName)) {
            file = Tools.renameDirectory(file, frmName);
        }

        fileService.fillNavigatorData(oldModel, file, true);

        oldModel.put("name", file.getName());
        oldModel.put("optnames", Arrays.stream(file.getParentFile().listFiles(File::isDirectory))
                        .map(File::getName)
                        .collect(Collectors.toList())
        );

        model.addAllAttributes(Tools.getDefaultModel("Bibliotheca - edit directory", null));
        model.addAllAttributes(oldModel);

        return "EditDirPage";
    }


    @RequestMapping("/editFile")
    public String editFile(
            @RequestParam("path") String path,
            @RequestParam(value = "saveClose", required = false) String saveClose,
            @RequestParam(value = "basename", required = false) String basename,
            @RequestParam(value = "tryDbKnih", required = false) String tryDbKnih,
            @RequestParam(value = "dbknih", required = false) String dbknih,
            @RequestParam(value = "bookname", required = false) String frmName,
            @RequestParam(value = "bookcover", required = false) String frmCover,
            @RequestParam(value = "bookdescription", required = false) String frmDescription,
            @RequestParam(value = "loadImage", required = false) String loadImage,
            @RequestParam(value = "loadDescription", required = false) String loadDescription,
            @RequestParam(value = "loadAll", required = false) String loadAll,
            @RequestParam(value = "loadAllClose", required = false) String loadAllClose,
            final Model model) {
        final Map<String, Object> oldModel = editFilePageService.getModel(path, basename, frmName, frmCover, frmDescription,
                dbknih, loadImage, loadDescription, loadAll, loadAllClose, tryDbKnih);
        model.addAllAttributes(oldModel);

        if (StringUtils.isNotBlank(saveClose) || StringUtils.isNotBlank(loadAllClose)) {
            return "redirect:browse?path="+ URLEncoder.encode(path);
        }

        return "EditFilePage";
    }

}
