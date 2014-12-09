import cuttings.CuttingsService
import cuttings.VOCutting
import graffiti.Get
import graffiti.Graffiti
import graffiti.Post
import groovy.io.FileType
import groovy.xml.MarkupBuilder
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.imgscalr.Scalr
import org.pegdown.PegDownProcessor

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.file.Paths
import java.util.zip.ZipFile
/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 * Date: 19.11.14
 */
class WebUI {

    public static final String CLASS_FILE = "file"
    public static final String CLASS_DIRECTORY = "directory"
    public static final String CLASS_EDIT_FORM = "edit-form"
    public static final String ID_NAVIGATOR = "navigator"
    public static final String ID_NAVIGATOR_FILE = "navigator-file"
    public static final int THUMBNAIL_SIZE = 200
    public static final String CLASS_COVER = "file-cover"
    public static final String CLASS_EXTENSION = "file-extension"
    public static final String CLASS_ACTION = "file-external_search"
    public static final String ID_DIRECTORIES = "directories"
    public static final String FRM_NAME = 'bookname'
    public static final String FRM_COVER = 'bookcover'
    public static final String FRM_AUTHOR = 'bookauthor'
    public static final String FRM_TITLE = 'booktitle'
    public static final String FRM_CALIBRE_SRC = 'bookcalibresrc'
    public static final String FRM_CALIBRE_TGT = 'bookcalibretgt'
    public static final String FRM_COPY_SRC = 'bookcopysrc'
    public static final String FRM_COPY_TGT = 'bookcopytgt'
    public static final String FRM_NAMES = "booknames"
    public static final String FRM_DESCRIPTION = "bookdescription"
    public static final String CLASS_EDIT_FORM_SECTION = "editFormSection"
    private final ConfigObject config
    def final CuttingsService cuttingService


    WebUI(ConfigObject config) {
        this.config = config

        cuttingService = new CuttingsService(config)
    }

    def run() {
        // static files served from here
        //       Graffiti.root 'public'

        // we also have to setup what static files to serve
        //       Graffiti.serve '*.css'

        Graffiti.config.port = 9192 // TODO Lebeda - přidat jako param

        // required to process annotations
        Graffiti.serve this

        // starting web server
        Graffiti.start()
    }

    @Get('/')
    def root() {
        def sb = new StringWriter()
        MarkupBuilder html = getHtmlBase(sb)
        html.html {
            head {
                meta(charset: "utf-8")
                title('Bibliotheca')
                //                script(src: 'test.js', type: 'text/javascript')
            }
            body {
                h1 "Bibliotheca" // TODO Lebeda - celkovy nadpis
                config.root.each { String path ->
                    p path
                    p { a(href: "browse?path=${path}", "$path") }
                }
            }
        }

        response.setCharacterEncoding('UTF-8')
        sb.toString()
    }

    @Get('/browse')
    def browse() {
        def sb = new StringWriter()
        MarkupBuilder html = getHtmlBase(sb)
        html.html {
            head {
                meta(charset: "utf-8")
                title('Bibliotheca')
                style getStyles()
                //                script(src: 'test.js', type: 'text/javascript')
            }
            body {
                def file = new File(params['path'])

                // navigator
                div(id: ID_NAVIGATOR) {
                    List<File> listOfFiles = getNavigatorData(file.parentFile)
                    div() {
                        listOfFiles.each {
                            text "/"
                            a(href: "browse?path=${it.absolutePath}", it.name)
                        }
                        text "/"
                        span(id: ID_NAVIGATOR_FILE) { mkp.yield(file.name) }
                    }
                }

                // akce nad adresářem
                div(style: " text-align: right; padding-right: 110px; padding-left: 110px") {
                    a(class: CLASS_ACTION, href: "https://www.google.cz/search?q=${file.name}", "Google",
                            target: "_blank")
                    a(class: CLASS_ACTION, href: "editDir?path=${file.absolutePath}", "Edit")
                }

                // adresare
                List<File> dirs = new ArrayList<>()
                file.eachDir { dirs.add(it) }
                dirs.sort()
                div(id: ID_DIRECTORIES) {
                    p {
                        dirs.each { File d ->
                            a(class: CLASS_DIRECTORY, href: "browse?path=${d.absolutePath}", d.name)
                        }
                    }
                }


                // soubory
                // group
                Map<String, List<File>> fileMap = new HashMap<>()
                file.eachFile(FileType.FILES) {
                    def basename = FilenameUtils.getBaseName(it.name)
                    if (!fileMap.containsKey(basename)) {
                        fileMap.put(basename, new ArrayList<File>())
                    }
                    fileMap.get(basename).add(it)
                }

                // ouput for group
                fileMap.keySet().sort().each { String basename ->
                    def files = fileMap.get(basename)

                    div(class: CLASS_FILE) {
                        p {
                            File cover = getCoverFile(files)
                            if (cover) {
                                img(class: CLASS_COVER, src: "getCover?path=${cover.absolutePath}",
                                        height: THUMBNAIL_SIZE, align: "Left")
                            }

                            h2 basename

                            File readme = getTypeFile(files, ".mkd")
                            if (readme) {
                                def pdp = new PegDownProcessor()
                                mkp.yieldUnescaped(pdp.markdownToHtml(readme.text))
                            }

                            files.sort().each { File f ->
                                String ext = FilenameUtils.getExtension(f.name)
                                if (!f.equals(cover) && !f.equals(readme)) {
                                    a(class: CLASS_EXTENSION, href: "download?path=${f.absolutePath}", ext)
                                }
                            }

                            a(class: CLASS_ACTION, href: "https://www.google.cz/search?q=${basename}", "Google",
                                    target: "_blank")
                            a(class: CLASS_ACTION, href: "editFile?path=${file.absolutePath}&basename=${basename}",
                                    "Edit")
                        }
                    }
                }

            }
        }

        response.setCharacterEncoding('UTF-8')
        sb.toString()
    }

    @Post('/editDir')
    @Get('/editDir')
    def editDir() {
        String pathName = params['path']

        def path = new File(pathName)

        // rename if need
        String frmName = params[FRM_NAME]
        if (frmName && !path.name.equals(frmName)) {
            File newPath = Paths.get(path.parentFile.absolutePath, frmName).toFile()
            if (newPath.exists()) {
                // move content and delete
                path.eachFile { File srcFile ->
                    File tgtFile = Paths.get(newPath.absolutePath, srcFile.name).toFile()
                    if (!tgtFile.exists()) {
                        FileUtils.moveToDirectory(srcFile, newPath, false)
                    } else {
                        def sha1Src = DigestUtils.sha1Hex(new FileInputStream(srcFile))
                        def sha1Tgt = DigestUtils.sha1Hex(new FileInputStream(tgtFile))

                        if (sha1Src != sha1Tgt) {
                            FileUtils.copyFile(srcFile,
                                    Paths.get(newPath.absolutePath, sha1Src + "_" + srcFile.name).toFile())
                        }
                        srcFile.delete()

                    }
                }
                FileUtils.deleteDirectory(path)
            } else {
                FileUtils.moveDirectory(path, newPath)
            }

            path = newPath
        }

        //        // download new cower if need
        //        File cover = getCoverFile(files)
        //        String baseFileName = Paths.get(path.getAbsolutePath(), basename).toString()
        //        String frmCover = params[FRM_COVER]
        //        if (frmCover) {
        //            URL website = new URL(frmCover);
        //            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        //            def covername = baseFileName + ".jpg"
        //            FileOutputStream fos = new FileOutputStream(covername);
        //            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        //            fos.close()
        //            cover = new File(covername)
        //        }

        //        // create description if need
        //        File readme = getTypeFile(files, ".mkd")
        //        String frmDescription = params[FRM_DESCRIPTION]
        //        if (frmDescription && (frmDescription != readme?.text)) {
        //            readme = new File(baseFileName + ".mkd")
        //            readme.write(frmDescription)
        //        }

        // output dir
        def sb = new StringWriter()
        MarkupBuilder html = getHtmlBase(sb)
        html.html {
            head {
                meta(charset: "utf-8") // TODO Lebeda -
                title('Edit ' + path.name)
                style getStyles()
                //                script(type:"application/javascript", src:"http://code.jquery.com/jquery-latest.min.js")
                //                script(src: 'test.js', type: 'text/javascript')
            }
            body {
                // záhlaví zobrazení
                div(id: ID_NAVIGATOR) {
                    List<File> listOfFiles = getNavigatorData(path.parentFile)
                    listOfFiles.each {
                        text "/"
                        a(href: "browse?path=${it.absolutePath}", it.name)
                    }
                    text "/"
                    a(id: ID_NAVIGATOR_FILE, href: "browse?path=${path.absolutePath}", path.name)
                    //                    span(id: ID_NAVIGATOR_FILE) { mkp.yield(path.name) }
                }

                div(class: CLASS_EDIT_FORM) {
                    form(action: "editDir?path=${path.absolutePath}", method: "post") {
                        // TODO Lebeda - přejmenovat/sloučit
                        div(class: CLASS_EDIT_FORM_SECTION) {
                            text "Name:"
                            input(type: "text", name: FRM_NAME, value: path.name, list: FRM_NAMES, size: 50)
                            datalist(id: FRM_NAMES) {
                                getNameSet(path.parentFile, FileType.DIRECTORIES).each {
                                    option(value: it)
                                }
                            }
                        }

                        //                        // TODO Lebeda - cover
                        //                        div(class: CLASS_EDIT_FORM_SECTION) {
                        //                            if (cover) {
                        //                                img(class: CLASS_COVER, style: "padding: 20", src: "getCover?path=${cover.absolutePath}",
                        //                                        height: THUMBNAIL_SIZE)
                        //                            }
                        //                            p {
                        //                                text "New cover:"
                        //                                input(type: "text", name: FRM_COVER, size: 50)
                        //                            }
                        //                        }

                        //                        // TODO Lebeda - description
                        //                        div(class: CLASS_EDIT_FORM_SECTION) {
                        //                            p "Description:"
                        //                            textarea(name: FRM_DESCRIPTION, cols: "50", rows: "5", style: "width: 100%") {
                        //                                if (readme) {
                        //                                    mkp.yieldUnescaped(readme.text)
                        //                                }
                        //                            }
                        //                        }

                        div(style: " text-align: right; padding: 20px") {
                            a(class: CLASS_ACTION, href: "https://www.google.cz/search?q=${path.name}", "Google",
                                    target: "_blank")
                            input(type: "submit", value: "save")
                        }
                    }
                }

            }
        }

        response.setCharacterEncoding('UTF-8')
        sb.toString()
    }

    @Post('/editFile')
    @Get('/editFile')
    def editFile() {
        String pathName = params['path']
        String basename = params['basename']

        def path = new File(pathName)
        List<File> files = new ArrayList<>()
        path.eachFile {
            if (FilenameUtils.getBaseName(it.absolutePath).equals(basename)) {
                files.add(it)
            }
        }

        // rename if need
        String frmName = params[FRM_NAME]
        if (frmName && !basename.equals(frmName)) {
            // rename all
            files.each {
                String path1 = it.absolutePath
                String path2 = it.absolutePath.replace(basename, frmName)
                FileUtils.moveFile(new File(path1), new File(path2))
            }

            // refresh names
            basename = frmName
            files.clear()
            path.eachFile {
                if (FilenameUtils.getBaseName(it.absolutePath).equals(basename)) {
                    files.add(it)
                }
            }
        }

        // download new cower if need
        File cover = getCoverFile(files)
        String baseFileName = Paths.get(path.getAbsolutePath(), basename).toString()
        String frmCover = params[FRM_COVER]
        if (frmCover) {
            URL website = new URL(frmCover);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            def covername = baseFileName + ".jpg"
            FileOutputStream fos = new FileOutputStream(covername);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close()
            cover = new File(covername)
        }

        // create description if need
        File readme = getTypeFile(files, ".mkd")
        String frmDescription = params[FRM_DESCRIPTION]
        if (frmDescription && (frmDescription != readme?.text)) {
            readme = new File(baseFileName + ".mkd")
            readme.write(frmDescription)
        }

        // conversion if wanted
        String calibreSrc = params[FRM_CALIBRE_SRC]
        String calibreTgt = params[FRM_CALIBRE_TGT]
        String calibreAuthor = params[FRM_AUTHOR]
        String calibreTitle = params[FRM_TITLE]
        if (calibreSrc) {
            List<String> calibreRun = new ArrayList<>()
            calibreRun.add("${config.external?.convert}")
            calibreRun.add(calibreSrc)
            calibreRun.add(baseFileName+calibreTgt)

            //  $CDP
            if (calibreSrc.toLowerCase().endsWith(".pdb")) {
                calibreRun.add("--input-encoding=cp1250 ")
            }

            if (cover) {
                calibreRun.add("--cover")
                calibreRun.add(cover.absolutePath)
            }

            if (calibreAuthor) {
                calibreRun.addAll("--authors")
                calibreRun.addAll(calibreAuthor)
            }

            if (calibreTitle) {
                calibreRun.add("--title")
                calibreRun.add(calibreTitle)
            }

            //~/bin/fixEpubJustify.sh "$TGT" && \
            //~/bin/fixEpubFont.sh "$TGT"

            println( calibreRun )
            def execute = calibreRun.execute()
            execute.waitFor()


            // refresh names
            files.clear()
            path.eachFile {
                if (FilenameUtils.getBaseName(it.absolutePath).equals(basename)) {
                    files.add(it)
                }
            }
        }

        // copy if wanted
        String copySrc = params[FRM_COPY_SRC]
        String copyTgt = params[FRM_COPY_TGT]
        if (copySrc && copyTgt) {
            File src = new File(copySrc)
            File tgt = new File(copyTgt)
            println("copy ${copySrc} to ${copyTgt}")
            FileUtils.copyFileToDirectory(src, tgt)
        }

        // output form
        def sb = new StringWriter()
        MarkupBuilder html = getHtmlBase(sb)
        html.html {
            head {
                meta(charset: "utf-8") // TODO Lebeda -
                title('Edit ' + basename)
                style getStyles()
                //                script(type:"application/javascript", src:"http://code.jquery.com/jquery-latest.min.js")
                //                script(src: 'test.js', type: 'text/javascript')
            }
            body {
                // záhlaví zobrazení
                div(id: ID_NAVIGATOR) {
                    List<File> listOfFiles = getNavigatorData(path)
                    listOfFiles.each {
                        text "/"
                        a(href: "browse?path=${it.absolutePath}", it.name)
                    }
                    text "/"
                    span(id: ID_NAVIGATOR_FILE) { mkp.yield(basename) }
                }

                div(class: CLASS_EDIT_FORM) {
                    form(action: "editFile?path=${path.absolutePath}&basename=${basename}", method: "post") {
                        // rename or move
                        div(class: CLASS_EDIT_FORM_SECTION) {
                            text "Name:"
                            input(type: "text", name: FRM_NAME, value: basename, list: FRM_NAMES, size: 50)
                            datalist(id: FRM_NAMES) {
                                getNameSet(path, FileType.FILES).each {
                                    option(value: it)
                                }
                            }
                        }

                        // cover
                        div(class: CLASS_EDIT_FORM_SECTION) {
                            if (cover) {
                                img(class: CLASS_COVER, style: "padding: 20",
                                        src: "getCover?path=${cover.absolutePath}",
                                        height: THUMBNAIL_SIZE)
                            }
                            p {
                                text "New cover:"
                                input(type: "text", name: FRM_COVER, size: 50)
                            }
                        }

                        // description
                        div(class: CLASS_EDIT_FORM_SECTION) {
                            p "Description:"
                            textarea(name: FRM_DESCRIPTION, cols: "50", rows: "5", style: "width: 100%") {
                                if (readme) {
                                    mkp.yieldUnescaped(readme.text)
                                }
                            }
                        }

                        // conversion with calibre
                        div(class: CLASS_EDIT_FORM_SECTION) {
                            table {
                                tr {
                                    td "Author:"
                                    td {
                                        input(type: "text", name: FRM_AUTHOR, size: 50,
                                                value: FilenameUtils.getBaseName(path.absolutePath))
                                    }
                                }
                                tr {
                                    td "Title:"
                                    td {
                                        input(type: "text", name: FRM_TITLE, size: 50, value: StringUtils.
                                                replace(basename, FilenameUtils.getBaseName(path.absolutePath) + " - ",
                                                        ""))
                                    }
                                }
                            }
                            p "Calibre convert"
                            table {
                                tr {
                                    td "Source:"
                                    td {
                                        select(name: FRM_CALIBRE_SRC) {
                                            option(value: "") { span "" }
                                            files.each { File file ->
                                                if (file.name.endsWith(".docx")
                                                        || file.name.endsWith(".epub")
                                                        || file.name.endsWith(".mobi")
                                                        || file.name.endsWith(".azw3")
                                                        || file.name.endsWith(".fb2")
                                                        || file.name.endsWith(".odt")
                                                        || file.name.endsWith(".pdb")
                                                        || file.name.endsWith(".txt")
                                                        || file.name.endsWith(".rtf")
                                                        || file.name.endsWith(".prc")
                                                        || file.name.endsWith(".pdf")
                                                ) {
                                                    option(value: file.absolutePath) { span file.name }
                                                }
                                            }
                                        }
                                    }
                                }
                                tr {
                                    td "Target:"
                                    td {
                                        select(name: FRM_CALIBRE_TGT) {
                                            option(value: ".fb2") { span ".fb2"}
                                            option(value: ".epub") { span ".epub"}
                                            option(value: ".mobi") { span ".mobi"}
                                        }
                                    }
                                }
                            }

                            // TODO Lebeda - conversion with
                            p "LibreOffice convert"
//                            libreoffice --headless --convert-to docx *.rtf
//                            libreoffice --headless --convert-to docx *.doc
//                            libreoffice --headless --convert-to docx *.odt

                        }

                        // TODO Lebeda - kopie do adresáře zařízení
                        div(class: CLASS_EDIT_FORM_SECTION) {
                            p "Copy to..."
                            table {
                                tr {
                                    td "source:"
                                    td {
                                        select(name: FRM_COPY_SRC) {
                                            option(value: "") { span "" }
                                            files.each { File file ->
                                               option(value: file.absolutePath) { span file.name }
                                            }
                                        }
                                    }
                                    td "target:"
                                    td {
                                        select(name: FRM_COPY_TGT) {
                                            option(value: "") { span "" }
                                            config.copyTargets?.each { String tgt ->
                                                File tgtFile = new File(tgt)
                                                option(value: tgtFile.absolutePath) { span tgtFile.name }
                                            }
                                        }

                                    }
                                }
                            }
                        }

                        // TODO Lebeda - delete files
                        div(class: CLASS_EDIT_FORM_SECTION) {
                            text "delete file"
                        }

                        div(style: " text-align: right; padding: 20px") {
                            a(class: CLASS_ACTION, href: "https://www.google.cz/search?q=${basename}", "Google",
                                    target: "_blank")
                            input(type: "submit", value: "save")
                        }
                    }
                }

            }
        }

        response.setCharacterEncoding('UTF-8')
        sb.toString()
    }

    private static Set<String> getNameSet(File path, FileType fileType) {
        Set<String> result = new HashSet<>()
        path.eachFile(fileType) {
            result.add(FilenameUtils.getBaseName(it.name))
        }
        result
    }

    private static File getCoverFile(files) {
        def cover = getTypeFile(files, ".jpg")

        if (!cover) {
            def file = getTypeFile(files, ".docx")
            try {
                if (file) {
                    ZipFile zip = new ZipFile(file);

                    //                zip.entries().each { entry ->
                    //                   println(entry.getName())
                    //                }

                    def entry = zip.getEntry("word/media/image1.jpg")
                    if (!entry) {
                        (
                                entry = zip.getEntry("word/media/image1.jpeg")
                        )
                    }

                    if (entry) {
                        InputStream is = zip.getInputStream(entry);

                        def thumbnailName = getThumbnailName(file)
                        IOUtils.copy(is, new FileOutputStream(thumbnailName))
                        cover = new File(thumbnailName)
                    }
                }
            } catch (e) {
                println(e)
            }
        }

        if (!cover) {
            def file = getTypeFile(files, ".epub")
            try {

                if (file) {
                    ZipFile zip = new ZipFile(file);

                    def entry = zip.getEntry("cover.jpg")
                    if (!entry) {
                        (
                                entry = zip.getEntry("cover.jpeg")
                        )
                    }
                    if (!entry) {
                        (
                                entry = zip.getEntry("images/image1.jpeg")
                        )
                    }
                    if (!entry) {
                        (
                                entry = zip.getEntry("images/image1.jpg")
                        )
                    }

                    if (entry) {
                        InputStream is = zip.getInputStream(entry);

                        def thumbnailName = getThumbnailName(file)
                        IOUtils.copy(is, new FileOutputStream(thumbnailName))
                        cover = new File(thumbnailName)
                    }
                }
            } catch (e) {
                println(e)
            }
        }

        if (!cover) {
            def file = getTypeFile(files, ".pdf")
            if (file) {
                try {
                    PDDocument document = PDDocument.load(file)
                    PDPage page = document.getDocumentCatalog().allPages[0]
                    if (page) {
                        BufferedImage image = page.convertToImage();
                        cover = new File(getThumbnailName(file))
                        ImageIO.write(image, "jpg", cover)
                    }
                } catch (e) {
                    println(e)
                }
            }
        }

        if (!cover) {
            def file = getTypeFile(files, ".docx")
            try {
                if (file) {
                    ZipFile zip = new ZipFile(file);

                    def entry = zip.entries().grep(~/^word\/media\/.*\.jpeg/).sort().get(0)
                    if (entry) {
                        InputStream is = zip.getInputStream(entry);

                        def thumbnailName = getThumbnailName(file)
                        IOUtils.copy(is, new FileOutputStream(thumbnailName))
                        cover = new File(thumbnailName)
                    }
                }
            } catch (e) {
                println(e)
            }
        }

        cover
    }

    private static String getThumbnailName(File file) {
        def thumbName = file.parent + File.separator + FilenameUtils.getBaseName(file.absolutePath) + ".jpg"
        thumbName
    }

    @Get('/getCover')
    def getCover() {
        def file = new File(params['path'])
        response.setContentType("image/jpeg")
        response.setHeader("Content-disposition", "attachment;filename=\"${file.name}\"")

        BufferedImage image = ImageIO.read(file);
        BufferedImage thumbnail = Scalr.resize(image, THUMBNAIL_SIZE);
        ImageIO.write(thumbnail, "jpg", response.outputStream);
    }

    private static File getTypeFile(List<File> files, suffix) {
        def grep = files.grep { File f ->
            f.name.endsWith(suffix)
        }
        grep[0]
    }

    private List<File> getNavigatorData(File file) {
        List<File> parentFiles = new ArrayList<>()
        String parentName = file.absolutePath
        String[] roots = config.root
        while (parentName.size() > 1) {
            def end = roots.contains(parentName)
            File parent = new File(parentName)
            parentFiles.add(parent)
            parentName = parent.parent
            if (end) {
                break
            }
        }
        def listOfFiles = parentFiles.reverse()
        listOfFiles
    }

    private static GString getStyles() {
        def linkBase = """
            border-width: 1px;
            border-style: solid;
            padding: 3px;
            border-radius: 4px;
        """

        """
        html {
            font-family: sans-serif;
        }
        a {
            color: black;
            visited: black;
            text-decoration: none
        }

        .${CLASS_DIRECTORY} {
            background: rgb(239, 240, 207);
            border-width: 1px;
            border-style: solid;
            padding: 10px;
            white-space: nowrap;
        }
        .${CLASS_FILE} {
           background: none repeat scroll 0% 0% #EFEFEF;
           margin: 20px 100px;
           border-width: 1px;
           border-style: solid;
           padding: 10px;
           min-height: 225px;
        }
        .${CLASS_EDIT_FORM} {
           background: none repeat scroll 0% 0% #EFEFEF;
           margin: 20px 100px;
           border-width: 1px;
           border-style: solid;
           padding: 10px;
        }
        .${CLASS_COVER} {
           padding-right: 10px;
           padding-bottom: 5px;
        }
        .${CLASS_EXTENSION} {
            ${linkBase}
            background: #C5F3F0;
            border-color: rgba(130, 239, 231, 0.84);
        }
        .${CLASS_ACTION} {
            ${linkBase}
            background: rgba(255, 239, 215, 1);
            border-color: rgba(239, 211, 130, 0.84);
        }
        .${CLASS_EDIT_FORM_SECTION} {
            border-bottom-width: 1px;
            border-bottom-style: dotted;
            padding: 10px;
        }

        #${ID_DIRECTORIES} {
            margin: 20px 100px;
            padding-left: 10px;
            line-height: 3;
        }
        #${ID_NAVIGATOR} {
            margin: 20px 100px;
            padding-left: 10px;
        }
        #${ID_NAVIGATOR_FILE} {
            font-weight: bold;
            font-size: x-large;
        }

        """
    }

    @Get('/download')
    def download() {
        def file = new File(params['path'])
        response.setContentType("application/octet-stream")
        response.setHeader("Content-disposition", "attachment;filename=\"${file.name}\"")
        response.outputStream << file.getBytes()
    }

    @Get('/about')
    def search() {
        def sb = new StringWriter()
        MarkupBuilder html = getHtmlBase(sb)
        html.html {
            head {
                meta(charset: "utf-8") // TODO Lebeda -
                title('About')
                //                script(src: 'test.js', type: 'text/javascript')
            }
            body {
                h1 "Bibliotheca"
                p "The software for browse and administer your archive."
                p "Příliš žluťoučký kůň úpěl ďábelské ódy."
            }
        }

        response.setCharacterEncoding('UTF-8')
        sb.toString()
    }

    @Get('/cuttings')
    def cuttings() {
        def sb = new StringWriter()
        MarkupBuilder html = getHtmlBase(sb)
        html.html {
            head {
                meta(charset: "utf-8") // TODO Lebeda -
                title('Cuttings')

                //                script(src: 'test.js', type: 'text/javascript')
            }
            body {
                cuttingService.getYears().each { String year ->
                    h1 "$year"

                    ul {
                        cuttingService.getCuttingsByYear(year).each { VOCutting cutting ->
                            li { a(href: "getCutting?id=${cutting.id}", cutting.name) }
                        }
                    }
                }
            }
        }

        response.setCharacterEncoding('UTF-8')
        sb.toString()
    }

    @Get('/getCutting')
    def getCutting() {
        Integer id = params['id'].toInteger()
        def cutting = cuttingService.mapCuttings.get(id)
        if (cutting) {
            response.setContentType("application/octet-stream")
            response.setHeader("Content-disposition", "attachment;filename=\"${cutting.name}\"")

            def file = new File(cutting.path)
            response.outputStream << file.getBytes()
        } else {
            writeError("Nenalezený soubor")
        }
    }

    def writeError(final String s) {
        def sb = new StringWriter()
        MarkupBuilder html = getHtmlBase(sb)
        html.html {
            head {
                title('Error')
                //                script(src: 'test.js', type: 'text/javascript')
            }
            body {
                h1 "Error"
                p s
            }
        }

        sb.toString()
    }

    /**
     * Build base for building html
     *
     * @param sb
     * @return
     */
    private MarkupBuilder getHtmlBase(StringWriter sb) {
        def html = new MarkupBuilder(sb)

        html.doubleQuotes = true
        html.expandEmptyElements = true
        html.omitEmptyAttributes = false
        html.omitNullAttributes = false
        //        html.encoding = "UTF-8"
        return html
    }
}
