package bibliotheca.service.impl;

import bibliotheca.config.ConfigService;
import bibliotheca.model.VOFile;
import bibliotheca.model.VOFileDetail;
import bibliotheca.model.VOPath;
import bibliotheca.service.*;
import bibliotheca.tools.Tools;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 15.12.14
 */
@Service
public class BrowsePageServiceImpl implements BrowsePageService {

    @Autowired
    private FileService fileService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private BookDetailService bookDetailService;

    @Autowired
    private DataBaseKnihService dataBaseKnihService;

    @Autowired
    private UuidService uuidService;

    @Override
    public Map<String, Object> getModel(String path, final String booksearch,  final String basename) {
        final HashMap<String, Object> model = Tools.getDefaultModel("Bibliotheca - Browse fiction", path);

        File file = new File(path);
        model.put(Tools.PARAM_PATH, file.getAbsolutePath());
        try {
            model.put("encodedPath", URLEncoder.encode(file.getAbsolutePath(), "UTF-8").replaceAll("%2F", "/"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }

        fileService.fillNavigatorData(model, file, false);

        // search
        model.put(Tools.FRM_SEARCH, booksearch);
        List<File> dirs = new ArrayList<>();
        if (StringUtils.isNotBlank(booksearch)) {
            final Collection<File> fileCollection = FileUtils.listFilesAndDirs(file, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
            CollectionUtils.filter(fileCollection, object ->
                    StringUtils.containsIgnoreCase(
                            Tools.removeDiacritics(object.getName()),
                            Tools.removeDiacritics(booksearch)));
            dirs.addAll(fileCollection);
        } else {
            final File[] listFiles = file.listFiles();
            if (listFiles != null) {
                dirs.addAll(Arrays.asList(listFiles));
            }
        }
        final ArrayList<VOPath> dirList = new ArrayList<>();
        final ArrayList<VOPath> fileList = new ArrayList<>();

        dirs.stream()
                .sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()))
                .forEach(f -> {
                    final VOPath voPath = new VOPath(f.getName(), f.getAbsolutePath());
                    if (f.isDirectory()) {
                        dirList.add(voPath);
                    } else {
                        fileList.add(voPath);
                    }
                });

        // adresare
        model.put("dirs", dirList);

        // soubory
        HashMap<String, List<VOFile>> fileMap = new HashMap<>();
        fileList.parallelStream()
                .filter(voPath -> !(
                                voPath.getPath().toLowerCase().endsWith("mht")
                                        || voPath.getPath().toLowerCase().endsWith("mhtml")
                                        || voPath.getName().contains(".bak2")
                        )
                )
                .forEachOrdered(voPath -> {
                    String basenamePath = FilenameUtils.getBaseName(voPath.getName());
                    if (!fileMap.containsKey(basenamePath)) {
                        fileMap.put(basenamePath, new ArrayList<>());
                    }
                    fileMap.get(basenamePath).add(new VOFile(
                            FilenameUtils.getExtension(voPath.getPath()),
                            voPath.getName(), voPath.getPath()));

                });

        List<VOFileDetail> fileDetails = new ArrayList<>();
        fileMap.entrySet().stream()
                .forEach(stringListEntry -> {
                    final String key = stringListEntry.getKey();
                    final List<VOFile> voFileList = stringListEntry.getValue();

                    @SuppressWarnings("unchecked")
                    final VOFileDetail fileDetail = bookDetailService.getVoFileDetail(path, key, voFileList);

                    fileDetails.add(fileDetail);
                });

        CollectionUtils.filter(fileDetails, object -> object != null);

        if (CollectionUtils.isNotEmpty(fileDetails)) {
            try {
                fileDetails.sort((o1, o2) -> new CompareToBuilder()
                        .append(o1.getBookauthor(), o2.getBookauthor())
                        .append(o1.getSerie(), o2.getSerie())
                        .append(o1.getTitle(), o2.getTitle())
                        .append(o1.getBookFileName(), o2.getBookFileName())
                        .toComparison());
            } catch (NullPointerException e) {
                System.out.println("nic");
            }
        }
        model.put("fileDetails", fileDetails);

        final List<VOPath> mhtFiles = fileList.stream()
                .filter(voPath -> (
                        voPath.getPath().toLowerCase().endsWith("mht")
                                || voPath.getPath().toLowerCase().endsWith("mhtml")))
                .sorted((o1, o2) -> o1.getName().compareTo(o2.getName()))
                .collect(Collectors.toList());
        model.put("mhtFiles", mhtFiles);

        return model;
    }


    @Override
    public Map<String, Object> tryDb(String path, String name) {
        final HashMap<String, Object> model = Tools.getDefaultModel("Bibliotheca - Browse fiction", path);

        File file = new File(path);
        model.put(Tools.PARAM_PATH, file.getAbsolutePath());
        try {
            model.put("encodedPath", URLEncoder.encode(file.getAbsolutePath(), "UTF-8").replaceAll("%2F", "/"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }

        VOFileDetail fd = bookDetailService.getVoFileDetail(path, name);
        dataBaseKnihService.tryDb(fd);

//        List<VOFileDetail> fileDetails = new ArrayList<>();
//        fileDetails.add(fd);
//        model.put("fileDetails", fileDetails);
        model.put("p", fd);

        return model;
    }

    @Override
    public Map<String, Object> loadItemModel(String path, String name) {
        final HashMap<String, Object> model = Tools.getDefaultModel("Bibliotheca - Browse fiction", path);

        File file = new File(path);
        model.put(Tools.PARAM_PATH, file.getAbsolutePath());
        try {
            model.put("encodedPath", URLEncoder.encode(file.getAbsolutePath(), "UTF-8").replaceAll("%2F", "/"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }

        VOFileDetail fd = bookDetailService.getVoFileDetail(path, name);
        model.put("p", fd);

        return model;
    }

}
