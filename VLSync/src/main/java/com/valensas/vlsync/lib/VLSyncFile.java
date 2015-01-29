package com.valensas.vlsync.lib;

import java.io.Serializable;

/**
 * This class represents file model referenced at
 * 'content.json' file's files field.
 *
 * @see com.valensas.vlsync.lib.VLSyncContentFile#files
 *
 * Created on 1/21/15
 * Created @ Valensas
 *
 * @author Furkan Bayraktar
 * @version 1.0
 * @since 1.0
 */
class VLSyncFile implements Serializable {

    /**
     * ETag string of referenced file.
     */
    private String etag;

    /**
     * Path string of referenced file.
     */
    private String path;

    /**
     * Size of the referenced file.
     */
    private long size;

    /**
     * Getter method for {@link #etag}
     *
     * @since 1.0
     *
     * @return {@link #etag}
     */
    public String getEtag() {
        return etag;
    }

    /**
     * Setter method for {@link #etag}
     *
     * @since 1.0
     *
     * @param etag eTag string
     */
    public void setEtag(String etag) {
        this.etag = etag;
    }

    /***
     * Getter method for {@link #path}
     *
     * @since 1.0
     *
     * @return {@link #path}
     */
    public String getPath() {
        return path;
    }

    /***
     * Setter method for {@link #path}
     *
     * @since 1.0
     *
     * @param path path string
     */
    public void setPath(String path) {
        this.path = path;
    }

    /***
     * Getter method for {@link #size}
     *
     * @since 1.0
     *
     * @return {@link #size}
     */
    public long getSize() {
        return size;
    }

    /***
     * Setter method for {@link #size}
     *
     * @since 1.0
     *
     * @param size size long
     */
    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "{ \"_class\":\"" + getClass().getName() + "\", \"etag\":\"" + etag + "\", \"path\":" + path + ", \"size\":" + size + " }";
    }
}
