package bibliotheca.service.impl;

import bibliotheca.config.ConfigService;
import bibliotheca.model.VOFile;
import bibliotheca.model.VOFileDetail;
import bibliotheca.model.VOPath;
import bibliotheca.service.BookDetailService;
import bibliotheca.service.BrowsePageService;
import bibliotheca.service.DataBaseKnihService;
import bibliotheca.service.FileService;
import bibliotheca.tools.Tools;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Paths;
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

    @Override
    public Map<String, Object> getModel(String path, final String booksearch, final String devicePath,
                                        final String target, final String tidyup, final String delete,
                                        final String basename, String tryDB) {
        final HashMap<String, Object> model = Tools.getDefaultModel("Bibliotheca - Browse fiction");

        File file = new File(path);
        model.put(Tools.PARAM_PATH, file.getAbsolutePath());
        try {
            model.put("encodedPath", URLEncoder.encode(file.getAbsolutePath(), "UTF-8").replaceAll("%2F", "/"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }

        // try DB action
        if (StringUtils.isNoneBlank(tryDB)) {
            dataBaseKnihService.tryDb(path, tryDB, file);
        }

        // tidyup some file
        if (StringUtils.isNoneBlank(tidyup)) {
            final File[] listFiles = file.listFiles((dir, name) -> name.startsWith(tidyup));
            Arrays.stream(listFiles).forEach(this::tidyUp);
        }

        // delete files
        if (StringUtils.isNoneBlank(delete)) {
            final File[] listFiles = file.listFiles((dir, name) -> name.startsWith(delete));
            Arrays.stream(listFiles).forEach(File::delete);
        }

        // generate
        if (StringUtils.isNotBlank(target)) {
            final File[] listFiles = file.listFiles((dir, name) -> name.startsWith(basename));
            List<VOFile> fileList = Arrays.stream(listFiles)
                    .map(file1 -> new VOFile(file1.getAbsolutePath()))
                    .collect(Collectors.toList());

            if (fileService.getTypeFile(fileList, target, false) == null) {
                fileService.getTypeFile(fileList, target, true);
            }
        }

        // to device
        if (StringUtils.isNotBlank(devicePath)) {
            final File[] listFiles = file.listFiles((dir, name) -> name.startsWith(basename));
            List<VOFile> fileList = Arrays.stream(listFiles)
                    .map(file1 -> new VOFile(file1.getAbsolutePath()))
                    .collect(Collectors.toList());

            final VOFile voFile = fileService.getTypeFile(fileList, target, false);
            if (voFile != null) {
                try {
                    FileUtils.copyFileToDirectory(new File(voFile.getPath()), new File(devicePath));
                } catch (IOException e) {
                    throw new IllegalStateException(e);

                }
            }
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
                                || voPath.getPath().toLowerCase().endsWith("mhtml")))
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
                        .append(o1.getNazev(), o2.getNazev())
                        .append(o1.getName(), o2.getName())
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

        // TODO Lebeda - generovat tagy pro šablonu
        return model;
    }

    // TODO Lebeda - do service
    private void tidyUp(final File fileUklid) {
        try {
            final String[] split = StringUtils.split(fileUklid.getName(), "-", 2);
            String author = StringUtils.trim(split[0]);
            File tgt = new File(bookDetailService.getTgtPathByAuthor(author));

            File tgtFile = Paths.get(tgt.getAbsolutePath(), fileUklid.getName()).toFile();

            if (tgtFile.exists()) {
                String sha1Src = DigestUtils.sha1Hex(new FileInputStream(fileUklid));
                String sha1Tgt = DigestUtils.sha1Hex(new FileInputStream(tgtFile));

                if (!sha1Src.equals(sha1Tgt)) {
                    FileUtils.moveFile(fileUklid, Paths.get(
                            tgtFile.getParent(),
                            FilenameUtils.getBaseName(fileUklid.getName()),
                            sha1Src,
                            FilenameUtils.getExtension(fileUklid.getName())).toFile());
                } else {
                    //noinspection ResultOfMethodCallIgnored
                    tgtFile.delete();
                }
            } else {
                FileUtils.moveToDirectory(fileUklid, tgt, true);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


}
