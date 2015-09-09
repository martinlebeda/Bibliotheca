package bibliotheca.service;

import bibliotheca.model.VOFile;
import bibliotheca.tools.Tools;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 17.12.14
 */
@Service
public class EditFilePageService {

    @Autowired
    private FileService fileService;

    public Map<String, Object> getModel(String path, String basename, final String frmName, final String frmCover, final String frmDescription) {
        final HashMap<String, Object> model = Tools.getDefaultModel("Bibliotheca - edit file");

        File file = new File(path);
        model.put(Tools.PARAM_PATH, file.getAbsolutePath());


        List<VOFile> voFileList = new ArrayList<>();
        final String finalBasename2 = basename;
        File[] listFiles = file.listFiles(pathname -> {
            return pathname.getName().startsWith(finalBasename2);
        });
        Arrays.stream(listFiles).forEach(file1 -> voFileList.add(new VOFile(file1.getAbsolutePath())));

        // rename if need
        if (StringUtils.isNoneBlank(frmName) && !basename.equals(frmName)) {
            Tools.renameAll(basename, listFiles, frmName);

            // refresh names
            basename = frmName;
            listFiles = fileService.refreshFiles(basename, file, voFileList);
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
            desc = Tools.createDescription(baseFileName, frmDescription);
        }

        fileService.fillNavigatorData(model, listFiles[0], false);

        model.put("name", basename);
        model.put("author", file.getName());
        model.put("optnames", Arrays.stream(file.listFiles(File::isFile))
                        .map(file1 -> FilenameUtils.getBaseName(file1.getName()))
                        .collect(Collectors.toSet())
        );

        model.put("desc", desc);
        model.put("cover", (cover != null ? cover.getPath() : null));

        return model;
    }


    private String getRawDesc(final List<VOFile> files) {
        VOFile readme = fileService.getTypeFile(files, "mkd", false);
        final String html;
        if (readme != null) {
            final File file = new File(readme.getPath());
            try {
                final FileInputStream input = new FileInputStream(file);
                final List<String> strings = IOUtils.readLines(input);
                html = StringUtils.join(strings, "\n");
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            html = null;
        }

        return html;
    }

}
