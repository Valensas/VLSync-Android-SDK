package com.valensas.vlsync.lib;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

/**
 * This class represents objects which handles a download task
 * for a specific file. When the download completed, {@link #mListener}
 * object is notified.
 * </br></br>
 * Created on 1/22/15</br>
 * Created @ Valensas
 *
 * @author Furkan Bayraktar
 * @version 1.0
 * @since 1.0
 */
class VLSyncDownloadTask {

    /**
     * Download URL.
     */
    private String mUrl;

    /**
     * Path to save downloaded file.
     */
    private String mPath;

    /**
     * Context to reach {@link android.app.DownloadManager}
     */
    private Context mContext;

    /**
     * Id of download task in {@link android.app.DownloadManager}
     */
    private long downloadID;

    /**
     * {@link android.app.DownloadManager} to download specified
     * file
     */
    private DownloadManager downloadManager;

    /**
     * Callback object to notify when download is completed.
     */
    private OnDownloadFinishedListener mListener;

    /**
     * Constructor method.
     *
     * @since 1.0
     *
     * @param url Download URL
     * @param path Path to save downloaded file
     * @param context Context to reach {@link android.app.DownloadManager}
     * @param listener Callback object to notify when download is completed
     */
    protected VLSyncDownloadTask(String url, String path, Context context, OnDownloadFinishedListener listener) {
        VLSync.log("Constructing download task...");
        if(url == null){
            VLSync.log("Url cannot be null.");
            throw new VLSyncException("Url cannot be null.");
        }
        if(path == null){
            VLSync.log("Path cannot be null.");
            throw new VLSyncException("Path cannot be null.");
        }
        if(context == null){
            VLSync.log("Context cannot be null.");
            throw new VLSyncException("Context cannot be null.");
        }
        if(listener == null){
            VLSync.log("Listener cannot be null.");
            throw new VLSyncException("Listener cannot be null.");
        }
        this.mUrl = url;
        this.mPath = path;
        this.mContext = context;
        this.mListener = listener;
        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        initializeDownloadManager();
        VLSync.log("Construction completed.");
    }

    /**
     * Initialization method for {@link #downloadManager}. It simply
     * registers a {@link android.content.BroadcastReceiver} to manager.
     *
     * @since 1.0
     */
    private void initializeDownloadManager(){
        VLSync.log("Initializing download manager...");
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                VLSync.log("Data received from download manager.");
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
                if (id != downloadID) {
                    VLSync.log("Id is not matched with currently downloaded file's id.");
                    return;
                }
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(id);
                Cursor cursor = downloadManager.query(query);

                // it shouldn't be empty, but just in case
                if (!cursor.moveToFirst()) {
                    return;
                }

                int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int x = cursor.getInt(statusIndex);
                if (DownloadManager.STATUS_SUCCESSFUL != x) {
                    VLSync.log("Download is failed.");
                    mListener.failed();
                }else{
                    VLSync.log("Download is completed successfully by download manager.");
                    mListener.success();
                }
            }
        }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        VLSync.log("Download manager initialization completed.");
    }

    /**
     * Used to start download process.
     *
     * @since 1.0
     */
    protected void download(){
        VLSync.log("Starting to download file at " + mUrl + ". File will be saved to " + mPath + ".");
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(mUrl));

        request.setTitle("VLSync");
        request.setDescription("Downloading content.json");

        request.setVisibleInDownloadsUi(false);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        }
        request.setDestinationInExternalFilesDir(mContext, null, mPath);

        downloadID = downloadManager.enqueue(request);
        VLSync.log("Download flow initiated.");
    }

    /**
     * Listener class to communicate with caller object.
     *
     * @since 1.0
     * @version 1.0
     */
    protected interface OnDownloadFinishedListener {

        /**
         * If download finished successfully, this method
         * is called.
         *
         * @since 1.0
         */
        public void success();

        /**
         * If download failed, this method is called.
         *
         * @since 1.0
         */
        public void failed();
    }
}
