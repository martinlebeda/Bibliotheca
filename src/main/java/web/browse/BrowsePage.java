package web.browse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.pegdown.PegDownProcessor;

import config.VOConfig;
import spark.Request;
import spark.Response;
import tools.Tools;
import web.AbstractPage;
import web.VOPath;


/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 15.12.14
 */
public class BrowsePage extends AbstractPage {

    public BrowsePage(final VOConfig config, final Request request, final Response response) {
        super(config, request, response);
    }

    @Override
    public Map<String, Object> getModel() {
        final HashMap<String, Object> model = getDefaultModel("Bibliotheca - Browse fiction");

        File file = new File(request.queryMap(PARAM_PATH).value());
        model.put(PARAM_PATH, file.getAbsolutePath());

        // tidyup some file
        String tidyUp = request.queryMap(TIDYUP).value();
        if (StringUtils.isNoneBlank(tidyUp)) {
            final File[] listFiles = file.listFiles((dir, name) -> name.startsWith(tidyUp));
            Arrays.stream(listFiles).forEach(this::tidyUp);
        }

        // delete files
        String delete = request.queryMap("delete").value();
        if (StringUtils.isNoneBlank(delete)) {
            final File[] listFiles = file.listFiles((dir, name) -> name.startsWith(delete));
            Arrays.stream(listFiles).forEach(File::delete);
        }

        // generate
        String basenameFrm = request.queryMap("basename").value();
        String target = request.queryMap("target").value();
        if (StringUtils.isNotBlank(target)) {
            final File[] listFiles = file.listFiles((dir, name) -> name.startsWith(basenameFrm));
            List<VOFile> fileList = Arrays.stream(listFiles)
                    .map(file1 -> new VOFile(file1.getAbsolutePath()))
                    .collect(Collectors.toList());

            if (getTypeFile(fileList, target, false) == null) {
                getTypeFile(fileList, target, true);
            }
        }

        // to device
        String devicePath = request.queryMap("devicePath").value();
        if (StringUtils.isNotBlank(devicePath)) {
            final File[] listFiles = file.listFiles((dir, name) -> name.startsWith(basenameFrm));
            List<VOFile> fileList = Arrays.stream(listFiles)
                    .map(file1 -> new VOFile(file1.getAbsolutePath()))
                    .collect(Collectors.toList());

            final VOFile voFile = getTypeFile(fileList, target, false);
            if (voFile != null) {
                try {
                    FileUtils.copyFileToDirectory(new File(voFile.getPath()), new File(devicePath));
                } catch (IOException e) {
                    throw new IllegalStateException(e);

                }
            }
        }

        fillNavigatorData(model, file, false);

        // search
        String booksearch = request.queryMap(FRM_SEARCH).value();
        model.put(FRM_SEARCH, booksearch);
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
                    String basename = FilenameUtils.getBaseName(voPath.getName());
                    if (!fileMap.containsKey(basename)) {
                        fileMap.put(basename, new ArrayList<>());
                    }
                    fileMap.get(basename).add(new VOFile(
                            FilenameUtils.getExtension(voPath.getPath()),
                            voPath.getName(), voPath.getPath()));

                });

        List<VOFileDetail> fileDetails = new ArrayList<>();
        fileMap.entrySet().parallelStream()
                .forEach(stringListEntry -> {
                    final String key = stringListEntry.getKey();
                    final List<VOFile> voFileList = stringListEntry.getValue();
                    final VOFile voFile = getCover(voFileList);
                    String cover = "";
                    if (voFile != null) {
                        cover = voFile.getPath();
                    }
                    final String desc = getDesc(voFileList);

                    final VOFileDetail fileDetail = new VOFileDetail(key, cover, desc);

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
                                            || "mkd".equalsIgnoreCase(object.getExt())
                                            || "mht".equalsIgnoreCase(object.getExt())
                                            || "mhtml".equalsIgnoreCase(object.getExt())
                                            || "htmlz".equalsIgnoreCase(object.getExt())
                                    )
                            )
                    );
                    fileDetails.add(fileDetail);

                    // TODO Lebeda - doc && !odt -> otd
                    // TODO Lebeda - doc && !docx -> docx
                    // !suf -> suff, epub, fb2, mobi
                    Arrays.stream(new String[] {"epub", "fb2", "mobi"}).forEach(s -> {
                        if (getTypeFile(voFileList, s, false) == null) {
                            fileDetail.getTargets().add(s);
                        }
                    });

                    fileDetail.getDevices().addAll(config.getDevices());

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

    private String getTgtPathByAuthor(String author) {
        String firstLetter = StringUtils.substring(author, 0, 1).toUpperCase();
        File beleTgt = new File(config.getFictionArchive());
        return Paths.get(beleTgt.getAbsolutePath(), firstLetter, author).toString();
    }

    private String getDesc(final List<VOFile> files) {
        VOFile readme = getTypeFile(files, "mkd", false);
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
