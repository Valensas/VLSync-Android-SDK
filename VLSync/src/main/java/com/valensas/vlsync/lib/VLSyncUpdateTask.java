package com.valensas.vlsync.lib;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class represents an update task. It's an asynchronous task.
 * </br></br>
 * Created on 1/21/15</br>
 * Created @ Valensas
 *
 * @see android.os.AsyncTask
 *
 * @author Furkan Bayraktar
 * @version 1.0
 * @since 1.0
 */
class VLSyncUpdateTask extends AsyncTask<Void, Integer, VLSyncUpdateTask.Result> implements VLSyncDownloadTask.OnDownloadFinishedListener{

    /**
     * {@link com.valensas.vlsync.lib.VLSync} instance reference.
     */
    private VLSync sync;

    /**
     * Downloaded 'content.json' file id for
     * {@link android.app.DownloadManager}
     */
    private long downloadID;

    /**
     * Files to be deleted after update task.
     */
    private VLSyncFile[] deletedFiles;

    /**
     * Files to be downloaded by update task.
     */
    private VLSyncFile[] allFiles;

    /**
     * Currently downloaded files index in {@link #allFiles}
     */
    private int currentFile;

    /**
     * Total size of all downloadable files
     */
    private long totalSize = 0;

    /**
     * Current total size of downloaded files
     */
    private long currentTotal = 0;

    /**
     * If content eTag is changed, its set to this
     * reference to be updated at the end of update
     * operation.
     */
    private String newContentETag;

    /**
     * Constructor method. Initializes fields and
     * {@link android.app.DownloadManager}'s receiver.
     *
     * @since 1.0
     */
    protected VLSyncUpdateTask(){
        VLSync.log("Constructing update task object...");
        this.sync = VLSync.getInstance();
        if(this.sync == null){
            VLSync.log("VLSync is not properly initialized.");
            throw new VLSyncException("VLSync is not properly initialized.");
        }
        initializeBroadcastReceiver();
        VLSync.log("Construction completed.");
    }

    /**
     * When download completed, this registered broadcast
     * receiver handles the process.
     *
     * @since 1.0
     */
    private void initializeBroadcastReceiver(){
        VLSync.log("Initializing broadcast receiver for download manager...");
        sync.getContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                VLSync.log("Data received from download manager.");
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
                if (id != downloadID) {
                    VLSync.log("Id is not matched with currently downloaded file's id.");
                    return;
                }
                DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
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
                    VLSyncError error = new VLSyncError();
                    error.setCode(4);
                    error.setMessage("Content file cannot be downloaded.");
                    sync.onPostExecute(false, error);
                    return;
                }

                int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                String downloadedPackageUriString = cursor.getString(uriIndex);

                sync.setProgress(1);

                VLSyncContentFile contentFile;

                try {
                    VLSync.log("Parsing content.json.");
                    File file = new File(URI.create(downloadedPackageUriString));
                    Gson gson = new Gson();
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    contentFile = gson.fromJson(br, VLSyncContentFile.class);
                }
                catch (IOException e) {
                    VLSync.log("Parsing failure.");
                    contentFile = null;
                }

                if(contentFile == null){
                    VLSync.log("Content file not found.");
                    VLSyncError error = new VLSyncError();
                    error.setCode(5);
                    error.setMessage("Content file not found.");
                    sync.onPostExecute(false, error);
                    return;
                }

                VLSyncContentFile oldContentFile = null;

                try {
                    File file = new File(sync.getContext().getExternalFilesDir(null), "/"+sync.getId()+"/content.json");
                    if(file.exists()) {
                        VLSync.log("Old content file found. Parsing...");
                        Gson gson = new Gson();
                        BufferedReader br = new BufferedReader(new FileReader(file));
                        oldContentFile = gson.fromJson(br, VLSyncContentFile.class);
                    }
                }
                catch (IOException e) {
                    VLSync.log("Parsing failure.");
                    oldContentFile = null;
                }

                File failed = new File(sync.getContext().getExternalFilesDir(null), "/temp/failed_content.json");

                if(oldContentFile == null) {
                    VLSync.log("Downloading files for the first time.");
                    totalSize = contentFile.getTotalSize();
                    allFiles = contentFile.getFiles();
                    currentFile = 0;

                    if (allFiles != null && allFiles.length > 0) {
                        if (failed.exists()) {
                            VLSync.log("Failed update task found. Processing old downloaded files.");
                            boolean c = true;
                            for (int i = 0; i < allFiles.length; i++) {
                                currentFile = i;
                                File f = new File(sync.getContext().getExternalFilesDir(null), "/temp/" + allFiles[i].getPath());
                                if (!f.exists()) {
                                    VLSyncDownloadTask task = new VLSyncDownloadTask(sync.getProjectURL() + allFiles[i].getPath(), "/temp/" + allFiles[i].getPath(), sync.getContext(), VLSyncUpdateTask.this);
                                    task.download();
                                    c = false;
                                    break;
                                } else {
                                    currentTotal += allFiles[i].getSize();
                                }
                            }
                            if (c) {
                                VLSync.log("All files found in content.json are already downloaded. Completing update task.");
                                success();
                            }
                        } else {
                            VLSync.log("Starting to download files in content.json.");
                            VLSyncDownloadTask task = new VLSyncDownloadTask(sync.getProjectURL() + allFiles[currentFile].getPath(), "/temp/" + allFiles[currentFile].getPath(), sync.getContext(), VLSyncUpdateTask.this);
                            task.download();
                        }
                    }else{
                        VLSync.log("No files found in content.json. Completing update task.");
                        success();
                    }
                } else {
                    VLSync.log("Updating files...");

                    ArrayList<VLSyncFile> newFiles = new ArrayList<VLSyncFile>();
                    newFiles.addAll(Arrays.asList(contentFile.getFiles()));
                    ArrayList<VLSyncFile> oldFiles = new ArrayList<VLSyncFile>();
                    oldFiles.addAll(Arrays.asList(oldContentFile.getFiles()));

                    ArrayList<VLSyncFile> downloadQueue = new ArrayList<VLSyncFile>();
                    ArrayList<VLSyncFile> deleteQueue = new ArrayList<VLSyncFile>();

                    totalSize = 0;

                    for (int i = 0; i < newFiles.size(); i++) {
                        VLSyncFile f = newFiles.get(i);
                        boolean check = true;
                        for (int j = 0; j < oldFiles.size(); j++) {
                            VLSyncFile ff = oldFiles.get(j);
                            if(f.getPath().equals(ff.getPath())){
                                if(!f.getEtag().equals(ff.getEtag())){
                                    VLSync.log("File added to download queue: " + f);
                                    downloadQueue.add(f);
                                    totalSize += f.getSize();
                                    check = false;
                                }
                                break;
                            }
                        }
                        if(check){
                            totalSize += f.getSize();
                            VLSync.log("File added to download queue: " + f);
                            downloadQueue.add(f);
                        }
                    }

                    for (int i = 0; i < oldFiles.size(); i++) {
                        VLSyncFile f = oldFiles.get(i);
                        boolean check = true;
                        for (int j = 0; j < newFiles.size(); j++) {
                            VLSyncFile ff = newFiles.get(j);
                            if(f.getPath().equals(ff.getPath())){
                                check = false;
                                break;
                            }
                        }
                        if(check){
                            VLSync.log("File added to delete queue: " + f);
                            deleteQueue.add(f);
                        }
                    }

                    deletedFiles = new VLSyncFile[deleteQueue.size()];
                    for (int i = 0; i < deleteQueue.size(); i++) {
                        deletedFiles[i] = deleteQueue.get(i);
                    }
                    allFiles = new VLSyncFile[downloadQueue.size()];
                    for (int i = 0; i < downloadQueue.size(); i++) {
                        allFiles[i] = downloadQueue.get(i);
                    }
                    currentFile = 0;

                    if (allFiles.length > 0) {
                        if (failed.exists()) {
                            boolean c = true;
                            for (int i = 0; i < allFiles.length; i++) {
                                currentFile = i;
                                File f = new File(sync.getContext().getExternalFilesDir(null), "/temp/" + allFiles[i].getPath());
                                if (!f.exists()) {
                                    VLSync.log("Starting to download file at " + sync.getProjectURL() + allFiles[i].getPath());
                                    VLSyncDownloadTask task = new VLSyncDownloadTask(sync.getProjectURL() + allFiles[i].getPath(), "/temp/" + allFiles[i].getPath(), sync.getContext(), VLSyncUpdateTask.this);
                                    task.download();
                                    c = false;
                                    break;
                                } else {
                                    currentTotal += allFiles[i].getSize();
                                }
                            }
                            if (c) {
                                VLSync.log("No files found for update in content.json. Completing update task.");
                                success();
                            }
                        } else {
                            VLSync.log("Starting to download file at " + sync.getProjectURL() + allFiles[currentFile].getPath());
                            VLSyncDownloadTask task = new VLSyncDownloadTask(sync.getProjectURL() + allFiles[currentFile].getPath(), "/temp/" + allFiles[currentFile].getPath(), sync.getContext(), VLSyncUpdateTask.this);
                            task.download();
                        }
                    }else{
                        VLSync.log("No files found in content.json. Completing update task.");
                        success();
                    }
                }

            }
        }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        VLSync.log("Initialization of broadcast receiver for download manager completed.");
    }

    @Override
    protected Result doInBackground(Void... params) {

        VLSync.log("Starting background task...");

        Result result = new Result();

        String urlString = sync.getProjectURL() + "content.json";
        String etag;
        try {
            VLSync.log("URL Connection establishing to " + urlString);
            URL url = new URL(urlString);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("HEAD");
            con.setInstanceFollowRedirects(false);
            con.connect();

            VLSync.log("URL Connection established to " + urlString);

            etag = con.getHeaderField("etag");

            VLSync.log("ETag received: " + etag);

        } catch (MalformedURLException e1) {
            VLSync.log("Exception on establishing URL connection to " + urlString, e1);
            result.error = new VLSyncError();
            result.error.setCode(1);
            result.error.setMessage(e1.getMessage());
            return result;
        } catch (IOException e) {
            VLSync.log("Exception on establishing URL connection to " + urlString, e);
            result.error = new VLSyncError();
            result.error.setCode(2);
            result.error.setMessage(e.getMessage());
            return result;
        }

        if(etag != null){
            if(etag.equals(sync.getContentETag())){
                VLSync.log("ETag is not changed.");
                publishProgress(99);
                result.success = true;
                return result;
            }else{
                VLSync.log("ETag is changed. Old eTag: " + sync.getContentETag() + ". New eTag: " + etag + ".");
                newContentETag = etag;
            }
        }
        VLSync.log("Starting to download file at " + urlString + ". File will be saved to /temp/content.json.");

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(urlString));

        request.setTitle("VLSync");
        request.setDescription("Downloading content.json");

        request.setVisibleInDownloadsUi(false);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        }

        File failed = new File(sync.getContext().getExternalFilesDir(null), "/temp/content.json");

        if(failed.exists()){
            VLSync.log("Previously failed update task found.");
            failed.renameTo(new File(sync.getContext().getExternalFilesDir(null), "/temp/failed_content.json"));
        }

        request.setDestinationInExternalFilesDir(sync.getContext(), null, "/temp/content.json");

        DownloadManager downloadManager = (DownloadManager) sync.getContext().getSystemService(Context.DOWNLOAD_SERVICE);
        downloadID = downloadManager.enqueue(request);
        VLSync.log("Download flow initiated.");
        VLSync.log("Background task finished.");
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        VLSync.log("Progress updated: " + values[0]);
        this.sync.setProgress(values[0]);
    }

    @Override
    protected void onPostExecute(Result result) {
        super.onPostExecute(result);
        if(result != null){
            VLSync.log("Update task finished " + result);
            sync.onPostExecute(result.success, result.error);
        }
    }

    @Override
    public void success() {
        if(allFiles == null || allFiles.length == 0){
            VLSync.log("No files found to download. Finishing update task...");

            File failed = new File(sync.getContext().getExternalFilesDir(null), "/temp/failed_content.json");
            if(failed.exists()){
                VLSync.log("Previously failed task found. Deleting...");
                failed.delete();
            }

            File source = new File(sync.getContext().getExternalFilesDir(null), "/temp");
            if(source.exists()){
                VLSync.log("Files found in temp folder. Moving files...");

                File target = new File(sync.getContext().getExternalFilesDir(null), "/"+sync.getId());

                try {
                    moveDirectory(source, target);
                    VLSync.log("Deleting temp folder...");
                    FileUtils.deleteDirectory(source);

                    if(deletedFiles != null && deletedFiles.length > 0){
                        VLSync.log("Deleting files from queue...");
                        for (VLSyncFile f : deletedFiles){
                            File temp = new File(sync.getContext().getExternalFilesDir(null), "/"+sync.getId()+"/" + f.getPath());
                            if(temp.exists()){
                                VLSync.log("Deleting file at " + temp.getAbsolutePath());
                                temp.delete();
                                File enclosing = temp.getParentFile();
                                if(enclosing !=null && enclosing.exists() && enclosing.isDirectory()){
                                    File[] filesInside = enclosing.listFiles();
                                    if(filesInside == null || filesInside.length == 0){
                                        VLSync.log("Deleting folder at " + enclosing.getAbsolutePath());
                                        FileUtils.deleteDirectory(enclosing);
                                    }
                                }
                            }
                        }
                        VLSync.log("Delete queue cleaned.");
                        deletedFiles = null;
                    }

                    VLSync.log("Update task finished successfully.");
                    sync.onPostExecute(true, null);
                    sync.updateContentETag(newContentETag);
                } catch (Exception e) {
                    VLSync.log("Moving files failed.", e);
                    VLSyncError error = new VLSyncError();
                    error.setCode(6);
                    error.setMessage(e.getMessage());
                    sync.onPostExecute(false, error);
                }
            }else{
                VLSync.log("Update task finished successfully.");
                sync.onPostExecute(true, null);
                sync.updateContentETag(newContentETag);
            }

            return;
        }
        currentTotal += allFiles[currentFile].getSize();
        sync.setProgress((int) ((100*currentTotal)/totalSize));
        if(currentFile == allFiles.length - 1){

            File failed = new File(sync.getContext().getExternalFilesDir(null), "/temp/failed_content.json");
            if(failed.exists()){
                VLSync.log("Previously failed task found. Deleting...");
                failed.delete();
            }
            VLSync.log("Moving files...");

            File source = new File(sync.getContext().getExternalFilesDir(null), "/temp");
            File target = new File(sync.getContext().getExternalFilesDir(null), "/"+sync.getId());

            try {
                moveDirectory(source, target);
                VLSync.log("Deleting temp folder...");
                FileUtils.deleteDirectory(source);

                if(deletedFiles != null && deletedFiles.length > 0){
                    VLSync.log("Deleting files from queue...");
                    for (VLSyncFile f : deletedFiles){
                        File temp = new File(sync.getContext().getExternalFilesDir(null), "/"+sync.getId()+"/" + f.getPath());
                        if(temp.exists()){
                            VLSync.log("Deleting file at " + temp.getAbsolutePath());
                            temp.delete();
                            File enclosing = temp.getParentFile();
                            if(enclosing !=null && enclosing.exists() && enclosing.isDirectory()){
                                File[] filesInside = enclosing.listFiles();
                                if(filesInside == null || filesInside.length == 0){
                                    VLSync.log("Deleting folder at " + enclosing.getAbsolutePath());
                                    FileUtils.deleteDirectory(enclosing);
                                }
                            }
                        }
                    }
                    VLSync.log("Delete queue cleaned.");
                    deletedFiles = null;
                }

                VLSync.log("Update task finished successfully.");
                sync.onPostExecute(true, null);
                sync.updateContentETag(newContentETag);
            } catch (Exception e) {
                VLSync.log("Moving files failed.", e);
                VLSyncError error = new VLSyncError();
                error.setCode(6);
                error.setMessage(e.getMessage());
                sync.onPostExecute(false, error);
            }
        }else{
            currentFile++;
            VLSync.log("Starting to download file at " + sync.getProjectURL() + allFiles[currentFile].getPath());
            VLSyncDownloadTask task = new VLSyncDownloadTask(sync.getProjectURL() + allFiles[currentFile].getPath(), "/temp/" + allFiles[currentFile].getPath(), sync.getContext(), VLSyncUpdateTask.this);
            task.download();
        }
    }

    /**
     * Moves all files in source directory to target directory.
     * Overwrites existing files in target.
     *
     * @since 1.0
     *
     * @param source source folder
     * @param target destination folder
     * @throws Exception if source or target is null
     */
    private void moveDirectory(File source, File target) throws Exception{
        if(source == null || !source.exists()){
            VLSync.log("Source path not found!");
            throw new Exception("Source path not found!");
        }

        if(target == null){
            VLSync.log("Target path not found!");
            throw new Exception("Target path not found!");
        }
        VLSync.log("Copying files from " + source.getAbsolutePath() + " to " + target.getAbsolutePath());

        if(!target.exists()){
            FileUtils.moveDirectory(source, target);
        }else{
            File[] files = source.listFiles();
            if(files != null){
                for (File f : files){
                    if(f.isDirectory()){
                        File dir = new File(target, f.getName());
                        moveDirectory(f, dir);
                    }else{
                        VLSync.log("Copying file " + f.getAbsolutePath() + " to " + target.getAbsolutePath());
                        FileUtils.copyFileToDirectory(f, target);
                    }
                }
            }
        }
    }

    @Override
    public void failed() {
        VLSyncError error = new VLSyncError();
        error.setCode(3);
        error.setMessage("Error downloading file: " + allFiles[currentFile].getPath());
        VLSync.log("Update task failed. " + error);
        sync.onPostExecute(false, error);
    }

    /**
     * This wrapper class used to return multiple values
     * when the AsyncTask completed.
     *
     * @since 1.0
     * @version 1.0
     */
    protected class Result {

        /**
         * Operation is successful or not
         */
        boolean success;

        /**
         * If operation is failed, cause of the
         * failure.
         */
        VLSyncError error;

        @Override
        public String toString() {
            return "{ \"_class\":\"" + getClass().getName() + "\", \"success\":\"" + success + "\", \"error\":\"" + error + "\" }";
        }
    }
}
