/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 * Date: 19.11.14
 */

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Paths


// set loglevel
Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
root.setLevel(Level.DEBUG);


// load config file
def configFileName = Paths.get(System.getProperty("user.home"), ".bibliotheca.groovy").toString()
def cfgFile = new File(configFileName)
def config = null
if (cfgFile.exists()) {
    config = new ConfigSlurper().parse(cfgFile.toURI().toURL())
}

WebUI webUI = new WebUI(config)
webUI.run()