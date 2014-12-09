package cuttings

import groovy.transform.CompileStatic


/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 * Date: 20.11.14
 */
@CompileStatic
class VOCutting {
    static int LastId = 0;

    int id
    String path
    String name
    String year

    VOCutting(File file) {
        this.id = LastId++
        path = file.absolutePath
        name = file.name
        year = file.parentFile.parentFile.name
    }
}
