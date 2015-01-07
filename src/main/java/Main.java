/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 15.12.14
 */

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.SparkBase.port;
import static spark.SparkBase.staticFileLocation;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import config.VOConfig;
import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;
import web.CoverPage;
import web.DownloadPage;
import web.ViewPage;
import web.browse.BrowsePage;
import web.editdir.EditDirPage;
import web.editfile.EditFilePage;
import web.main.MainPage;


public class Main {

    // config file
    private static final String CONFIG_FILE = ".bibliotheca.xml";

    // TODO Lebeda - change Main to another class, this rename to run
    public static void main(String[] args) throws FileNotFoundException {
        // load configuration
        String homeDir = System.getProperty("user.home");
        final File configFile = Paths.get(homeDir, CONFIG_FILE).toFile();
        // TODO Lebeda - check if exists configFile

        Serializer serializer = new Persister();
        final VOConfig config;
        try {
            config = serializer.read(VOConfig.class, configFile);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        // only for quick generate
//        try {
//            serializer.write(config, configFile);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        //        InputStream input = getClass().getResourceAsStream("/classpath/to/my/file");
        //**************************************************//

        // set server
        port(config.getPort());
        staticFileLocation("/public"); // Static files

//        before((request, response) -> {
//          boolean authenticated = request.queryParams("password").equals("secret");
//          if(!authenticated){
//            halt(403, "Incorrect password, hacker alert, hacker alert!!!!");
//          }
//        });

        // set paths
//        get("/", (req, res) -> new MainPage().content(req, res));
        get("/", (request, response) -> {
            return new ModelAndView(new MainPage(config, request, response).getModel(), "MainPage.ftl");
        }, new FreeMarkerEngine());

        get("/browse", (request, response) -> {
            return new ModelAndView(new BrowsePage(config, request, response).getModel(), "BrowsePage.ftl");
        }, new FreeMarkerEngine());

        get("/editDir", (request, response) -> {
            return new ModelAndView(new EditDirPage(config, request, response).getModel(), "EditDirPage.ftl");
        }, new FreeMarkerEngine());
        post("/editDir", (request, response) -> {
            return new ModelAndView(new EditDirPage(config, request, response).getModel(), "EditDirPage.ftl");
        }, new FreeMarkerEngine());

        get("/editFile", (request, response) -> {
            return new ModelAndView(new EditFilePage(config, request, response).getModel(), "EditFilePage.ftl");
        }, new FreeMarkerEngine());
        post("/editFile", (request, response) -> {
            return new ModelAndView(new EditFilePage(config, request, response).getModel(), "EditFilePage.ftl");
        }, new FreeMarkerEngine());

        get("/cover", (req, res) -> new CoverPage(config, req, res).getModel());
        get("/download", (req, res) -> new DownloadPage(config, req, res).getModel());

        get("/view/*", (request, response) -> new ViewPage(config, request, response).getData() );
    }
}
