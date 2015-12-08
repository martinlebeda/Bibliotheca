package bibliotheca.service;

import bibliotheca.model.VOUuid;

/**
 * Routines for manipulate with uuid of books.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 4.12.15
 */
public interface UuidService {


    /**
     * Load from disk (.uuid file) or generate new uuid for book
     *
     * @param path path where book is stored
     * @param key name of book (filename without path and suffix)
     * @return uuid for this book
     */
    String getUuid(String path, String key);

    /**
     * Find VO by uuid in cache.
     *
     * @param uuid uuid of book
     * @return VO by uuid
     */
    VOUuid getByUuid(String uuid);

    /**
     * Remove ID from cache.
     * ie. if book is deleted or joined to another.
     * @param id id of book
     */
    void removeFromCache(String id);
}
