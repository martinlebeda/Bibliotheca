package bibliotheca.service;

import bibliotheca.config.ConfigService;
import bibliotheca.model.VOFile;
import bibliotheca.model.VOFileDetail;
import bibliotheca.model.VOPath;
import bibliotheca.tools.Tools;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.pegdown.PegDownProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 15.12.14
 */
@Service
public class BrowsePageService {

    @Autowired
    private FileService fileService;

    @Autowired
    private ConfigService configService;

    public Map<String, Object> getModel(String path, final String booksearch, final String devicePath,
                                        final String target, final String tidyup, final String delete,
                                        final String basename, String tryDB) {
        final HashMap<String, Object> model = Tools.getDefaultModel("Bibliotheca - Browse fiction");

        File file = new File(path);
        model.put(Tools.PARAM_PATH, file.getAbsolutePath());

        // try DB action
        if (StringUtils.isNoneBlank(tryDB)) {
            try {
                String bookname = StringUtils.replacePattern(tryDB, ".*- *", "");
                Map<String, String> metadata = Tools.getStringStringMap(path, tryDB);
                String dbKnihUrl = Tools.getAutomaticDBKnihUrl(bookname);
                if (StringUtils.isNotBlank(dbKnihUrl)) {
                    metadata.put(Tools.METADATA_KEY_DATABAZEKNIH_CZ, dbKnihUrl);
                    Tools.writeMetaData(path, tryDB, metadata);

                    Document doc = Jsoup.connect(metadata.get(Tools.METADATA_KEY_DATABAZEKNIH_CZ)).get();
                    String description = Tools.getDBKnihDescription(doc);

                    if (StringUtils.isNotBlank(description)) {
                        String baseFileName = Paths.get(file.getAbsolutePath(), tryDB).toString();
                        Tools.createDescription(baseFileName, description);
                    }
                }

            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
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
                                ; // nothing
                            }
                        }
                        // TODO Lebeda - odmazat příznak
                    } else {
                        if (!Files.exists(noCoverPath)) {
                            try {
                                FileOutputStream fos = new FileOutputStream(nocovername);
                                fos.write("no cover".getBytes());
                                fos.close();
                            } catch (IOException e) {
                                ; // nothing
                            }
                        }
                    }

                    final String desc = getDesc(voFileList);

                    Map<String, String> metadata = Tools.getStringStringMap(path, FilenameUtils.getBaseName(voFileList.get(0).getName()));

                    final VOFileDetail fileDetail = new VOFileDetail(key, cover, desc, metadata.get(Tools.METADATA_KEY_DATABAZEKNIH_CZ));

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
                                            || "mhtml".equalsIgnoreCase(object.getExt())
                                            || "htmlz".equalsIgnoreCase(object.getExt())
                                    )
                            )
                    );
                    fileDetails.add(fileDetail);

                    // TODO Lebeda - doc && !odt -> otd
                    // TODO Lebeda - doc && !docx -> docx
                    // !suf -> suff, epub, fb2, mobi
                    Arrays.stream(new String[]{"epub", "fb2", "mobi"}).forEach(s -> {
                        if (fileService.getTypeFile(voFileList, s, false) == null) {
                            fileDetail.getTargets().add(s);
                        }
                    });

                    fileDetail.getDevices().addAll(configService.getConfig().getDevices());

                });

        CollectionUtils.filter(fileDetails, new Predicate<VOFileDetail>() {
            @Override
            public boolean evaluate(final VOFileDetail object) {
                return object != null;
            }
        });

        if (CollectionUtils.isNotEmpty(fileDetails)) {
            try {
                fileDetails.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
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
            File tgt = new File(getTgtPathByAuthor(author));

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
                    tgtFile.delete();
                }
            } else {
                FileUtils.moveToDirectory(fileUklid, tgt, true);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // TODO Lebeda - do service
    private String getTgtPathByAuthor(String author) {
        String firstLetter = StringUtils.substring(author, 0, 1).toUpperCase();
        File beleTgt = new File(configService.getConfig().getFictionArchive());
        return Paths.get(beleTgt.getAbsolutePath(), firstLetter, author).toString();
    }

    // TODO Lebeda - do service
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


}
