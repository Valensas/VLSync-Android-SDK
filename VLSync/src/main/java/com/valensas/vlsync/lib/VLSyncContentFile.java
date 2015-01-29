package com.valensas.vlsync.lib;

import java.io.Serializable;

/**
 * This class represents content file model parsed from
 * 'content.json' file downloaded from VLSync server.
 *
 * Created on 1/21/15
 * Created @ Valensas
 *
 * @author Furkan Bayraktar
 * @version 1.0
 * @since 1.0
 */
class VLSyncContentFile implements Serializable {

    /**
     * Files referenced in 'content.json' file
     */
    private VLSyncFile[] files;

    /**
     * Last update date of 'content.json' file
     */
    private long lastUpdatedDate;

    /**
     * Getter method for {@link #files}
     *
     * @since 1.0
     *
     * @return array of files if any or null
     */
    public VLSyncFile[] getFiles() {
        return files;
    }

    /**
     * Setter method for {@link #files}
     *
     * @since 1.0
     *
     * @param files array of files
     */
    public void setFiles(VLSyncFile[] files) {
        this.files = files;
    }

    /**
     * Getter method for {@link #lastUpdatedDate}
     *
     * @since 1.0
     *
     * @return last update date in milliseconds
     */
    public long getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    /**
     * Setter method for {@link #lastUpdatedDate}
     *
     * @since 1.0
     *
     * @param lastUpdatedDate in milliseconds
     */
    public void setLastUpdatedDate(long lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    /**
     * Calculate total size of all files in this content
     * file.
     *
     * @return total size of {@link #files}
     */
    protected long getTotalSize(){
        long total = 0;
        for (VLSyncFile file : files){
            total += file.getSize();
        }
        VLSync.log("Total size is " + total);
        return total;
    }

    @Override
    public String toString() {

        String result = "{ \"_class\":\"" + getClass().getName() + "\", \"lastUpdatedDate\":" + lastUpdatedDate + ", \"files\":[";

        for (int i = 0; i < files.length; i++) {
            if(i == files.length - 1){
                result += files[i];
            }else{
                result += files[i] + ", ";
            }
        }

        result += "]}";

        return result;
    }
}
