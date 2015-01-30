package com.valensas.vlsync.lib;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * VLSync is used to handle file synchronization between
 * your mobile application and your VLSync project on web.
 * <br/><br/>
 * Created on 1/19/15<br/>
 * Created @ Valensas
 *
 * @see <a href="http://vlsync.valensas.com" target="_blank">VLSync Portal</a> for more information
 *
 * @author Furkan Bayraktar
 * @version 1.0
 * @since 1.0
 */
public class VLSync {

    /**
     * Date formatter.
     */
    private static final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    /**
     * S3 root URL.
     */
    protected static final String S3_URL = "http://v-sync.s3-website-eu-west-1.amazonaws.com/";

    /**
     * Debug Tag
     */
    private static final String TAG = "VLSync";

    /**
     * Debug flag
     */
    private static boolean mDebugEnabled = false;

    /**
     * Project root URL.
     */
    private String mProjectURL;

    /**
     * Singleton instance of VLSync
     */
    private static VLSync instance;

    /**
     * Project Id from VLSync project on web.
     */
    private String mId = null;

    /**
     * Reference to OnUpdateListener.
     * @see com.valensas.vlsync.lib.VLSync.OnUpdateListener
     */
    private OnUpdateListener mListener = null;

    /**
     * Last successful update operation date and time in
     * milliseconds.
     */
    private long mLastUpdate = -1;

    /**
     * Current update progress. Between 0 and 100 inclusive.
     */
    private int mProgress = -1;

    /**
     * Currently active HUD.
     */
    private VLSyncHUD mHUD = null;

    /**
     * Shared preferences instance
     */
    private SharedPreferences mSharedPrefs;

    /**
     * Application Context.
     */
    private Context mContext;

    /**
     * Default options.
     *
     * @see #update(java.util.Map)
     * @see #update(java.util.Map, android.content.Context)
     */
    private Map<UpdateOptionKey, UpdateOptionValue> defaultOptions = null;

    /**
     * Custom option flag.
     *
     * @see #update(java.util.Map)
     * @see #update(java.util.Map, android.content.Context)
     */
    private boolean customOptionsUsed = false;

    /**
     * Flag whether VLSync currently updating files or not.
     */
    private boolean updating = false;

    /**
     * Flag whether to display HUD or not;
     */
    private boolean showHUD = false;

    /**
     * Flag whether to display progress or not;
     */
    private boolean showProgress = false;

    /**
     * Flag whether to display progress text or not;
     */
    private boolean showProgressText = false;

    /**
     * Private constructor for VLSync called by {@link #initWithProjectId}
     *
     * @since 1.0
     *
     * @param applicationContext {@link android.app.Activity#getApplicationContext()}
     * @param id Project Id from VLSync project on web. If the id
     *           is null, VLSyncException is thrown.
     *
     */
    private VLSync(Context applicationContext, String id){
        if(id == null){
            log("VLSync cannot be initialized with null project id.");
            throw new VLSyncException("VLSync cannot be initialized with null project id.");
        }
        if(applicationContext == null){
            log("VLSync cannot be initialized with null application context.");
            throw new VLSyncException("VLSync cannot be initialized with null application context.");
        }
        this.mContext = applicationContext;
        this.mId = id;
        this.mProjectURL = S3_URL + this.mId + "/";
        mSharedPrefs = mContext.getSharedPreferences("com.valensas.vlsync.lib", 0);
        log("VLSync instance created.");
    }

    /**
     * Get current VLSync instance if it is already initialized.
     *
     * @since 1.0
     *
     * @return VLSync object if VLSync is properly initialized. If not,
     * VLSyncException is thrown.
     */
    public static VLSync getInstance(){
        if(instance == null){
            log("VLSync is not properly initialized.");
            throw new VLSyncException("VLSync is not properly initialized.");
        }
        return instance;
    }

    /**
     * Initializer method for VLSync. It must be called before VLSync
     * is used.
     *
     * @since 1.0
     *
     * @param id Project Id from VLSync project on web. It cannot be
     *           null.
     * @param applicationContext {@link android.app.Activity#getApplicationContext()}
     * @return VLSync object.
     */
    public static VLSync initWithProjectId(@NonNull String id, @NonNull Context applicationContext){
        log("Initialization started.");
        instance = new VLSync(applicationContext, id);
        log("Initialization completed.");
        return instance;
    }

    /**
     * Starts update process for files. Detects file changes between
     * VLSync server and application content. Downloads changes from
     * server.
     *
     * @since 1.0
     */
    public void update(){
        if(updating){
            log("Already updating... Update call ignored.");
            return;
        }
        updating = true;
        log("Update started.");

        if(mListener != null) {
            mListener.onPreUpdate();
        }
        new VLSyncUpdateTask().execute();
    }

    /**
     * Starts update process for files. Detects file changes between
     * VLSync server and application content. Downloads changes from
     * server.
     *
     * @since 1.0
     *
     * @param context necessary to display HUD
     */
    public void update(Context context){
        if(context == null){
            update();
            return;
        }
        if(updating){
            log("Already updating... Update call ignored.");
            return;
        }
        updating = true;
        log("Update started.");
        if(showHUD){
            mHUD = VLSyncHUD.show(context, showProgressText, showProgress);
        }
        if(mListener != null) {
            mListener.onPreUpdate();
        }
        new VLSyncUpdateTask().execute();
    }

    /**
     * Starts update process with options. If
     * {@link com.valensas.vlsync.lib.VLSync.HUDState} is set to
     * {@link com.valensas.vlsync.lib.VLSync.HUDState#VISIBLE}, a
     * {@link android.content.Context} reference must be passed to
     * update function in order to show progress HUD.
     *
     * @see #update()
     * @see com.valensas.vlsync.lib.VLSync.UpdateOptionKey
     * @see com.valensas.vlsync.lib.VLSync.HUDState
     * @since 1.0
     *
     * @param options desired options. If the same option given twice,
     *                last one is set.
     */
    public void update(Map<UpdateOptionKey, UpdateOptionValue> options){
        if(updating){
            log("Already updating... Update call ignored.");
            return;
        }
        log("Setting custom update options...");
        customOptionsUsed = true;
        setUpdateOptions(options);
        update();
    }

    /**
     * Starts update process with options. If
     * {@link com.valensas.vlsync.lib.VLSync.HUDState} is set to
     * {@link com.valensas.vlsync.lib.VLSync.HUDState#VISIBLE}, a
     * {@link android.content.Context} reference must be passed to
     * update function in order to show progress HUD.
     *
     * @see #update(android.content.Context)
     * @see com.valensas.vlsync.lib.VLSync.UpdateOptionKey
     * @see com.valensas.vlsync.lib.VLSync.HUDState
     * @since 1.0
     *
     * @param options desired options. If the same option given twice,
     *                last one is set.
     * @param context necessary to display HUD
     */
    public void update(Map<UpdateOptionKey, UpdateOptionValue> options, Context context){
        if(updating){
            log("Already updating... Update call ignored.");
            return;
        }
        log("Setting custom update options...");
        customOptionsUsed = true;
        setUpdateOptions(options);
        update(context);
    }

    /**
     * Set update options. If {@link com.valensas.vlsync.lib.VLSync.HUDState}
     * is set to {@link com.valensas.vlsync.lib.VLSync.HUDState#VISIBLE}, a
     * {@link android.content.Context} reference must be passed to update
     * function in order to show progress HUD.
     *
     * @see com.valensas.vlsync.lib.VLSync.UpdateOptionKey
     * @see com.valensas.vlsync.lib.VLSync.HUDState
     * @since 1.0
     *
     * @param options desired options. If the same option given twice,
     *                last one is set.
     */
    public void setUpdateOptions(Map<UpdateOptionKey, UpdateOptionValue> options){
        if(options == null){
            log("Options are null. Ignoring update options method call...");
            return;
        }
        if (!customOptionsUsed) {
            log("Setting default options...");
            this.defaultOptions = options;
        }

        for (UpdateOptionKey key : options.keySet()){
            switch (key){
                case PROGRESS_STYLE:
                    switch ((ProgressStyle) options.get(key)){
                        case DETERMINATE_TEXT_HIDDEN:
                            log("Progress is set determinate.");
                            log("Progress text is set hidden.");
                            this.showProgress = true;
                            this.showProgressText = false;
                            break;
                        case DETERMINATE_TEXT_VISIBLE:
                            log("Progress is set determinate.");
                            log("Progress text is set visible.");
                            this.showProgress = true;
                            this.showProgressText = true;
                            break;
                        case INDETERMINATE_TEXT_HIDDEN:
                            log("Progress is set indeterminate.");
                            log("Progress text is set hidden.");
                            this.showProgress = false;
                            this.showProgressText = false;
                            break;
                        case INDETERMINATE_TEXT_VISIBLE:
                            log("Progress is set indeterminate.");
                            log("Progress text is set visible.");
                            this.showProgress = false;
                            this.showProgressText = true;
                            break;
                    }
                    break;
                case HUD_STATE:
                    switch ((HUDState)options.get(key)){
                        case VISIBLE:
                            log("HUD is set visible.");
                            this.showHUD = true;
                            break;
                        case HIDDEN:
                            log("HUD is set hidden.");
                            this.showHUD = false;
                            break;
                    }
                    break;
            }
        }
    }

    /**
     * Current progress if there is an active update process.
     *
     * @since 1.0
     *
     * @return int value of progress between 0-100 inclusive.
     * If there is no active update process, returns -1;
     */
    public int progress(){
        if(updating) {
            log("Current progress: " + mProgress);
            return mProgress;
        }else{
            log("Current progress: -1");
            return -1;
        }
    }

    /**
     * Last successful update date and time.
     *
     * @since 1.0
     *
     * @return last update date and time in milliseconds.
     */
    public long lastUpdate(){
        log("Last update: " + formatter.format(new Date(mLastUpdate)));
        return mLastUpdate;
    }

    /**
     * Root folder for files received from VLSync servers.
     *
     * @since 1.0
     *
     * @return root folder of VLSync
     */
    public File getRootFolder(){
        File root = new File(mContext.getExternalFilesDir(null), "/"+mId+"/contents");
        log("Root folder is: " + root.getAbsolutePath());
        return root;
    }

    /**
     * Setter for OnUpdateListener.
     *
     * @see com.valensas.vlsync.lib.VLSync.OnUpdateListener
     * @since 1.0
     *
     * @param listener OnUpdateListener object.
     */
    public void setOnUpdateListener(OnUpdateListener listener){
        if(listener == null) {
            log("Update listener set as null");
        }else{
            log("Update listener set as " + listener.getClass().getName());
        }
        this.mListener = listener;
    }

    /**
     * If debugging enabled, debug messages will be printed
     * to console.
     *
     * @since 1.0
     *
     * @param enabled boolean value indicates debug status
     */
    public void setDebugEnabled(boolean enabled){
        log("Debugging " + (enabled ? "enabled" : "disabled"));
        mDebugEnabled = enabled;
    }

    /**
     * Setter method for {@link #mProgress}
     *
     * @since 1.0
     *
     * @param progress updated progress value. Must be between
     *                 0 and 100 inclusive.
     */
    protected void setProgress(int progress){
        if(progress >= 0 && progress <= 100){
            log("Progress set to " + progress);
            this.mProgress = progress;
            if(mHUD != null){
                mHUD.updateProgress(progress, "Syncing");
            }
            if(mListener != null){
                mListener.onProgressUpdate(progress);
            }
        }else{
            log("Invalid progress value: " + progress);
        }
    }

    /**
     * Called when VLSyncUpdateTask finishes update.
     *
     * @param success status of update
     * @param error if success is false, a VLSyncError is passed,
     *              otherwise null.
     */
    protected void onPostExecute(boolean success, VLSyncError error){
        if(success){
            this.mLastUpdate = new Date().getTime();
        }
        if(customOptionsUsed){
            log("Setting options back to default values.");
            customOptionsUsed = false;
            if(this.defaultOptions != null) {
                setUpdateOptions(this.defaultOptions);
            }else{
                this.showHUD = false;
                this.showProgress = false;
                this.showProgressText = false;
            }
        }
        if(mHUD != null){
            mHUD.dismiss();
            mHUD = null;
        }
        updating = false;
        log("Update operation " + (success ? "successful." : ("failed. Cause: " + error.getMessage())));

        if(mListener != null){
            mListener.onPostUpdate(success, error);
        }
    }

    /**
     * Returns project URL
     *
     * @see #mProjectURL
     * @since 1.0
     *
     * @return URL string of project.
     */
    protected String getProjectURL(){
        return mProjectURL;
    }

    /**
     * Update eTag of content.json
     *
     * @since 1.0
     *
     * @param eTag eTag of content.json
     */
    protected void updateContentETag(String eTag){
        log("Updating content.json's eTag as: " + eTag);
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putString("com.valensas.vlsync.lib.etag", eTag);
        editor.apply();
    }

    /**
     * @since 1.0
     *
     * @return eTag of content.json or null
     */
    protected String getContentETag(){
        return mSharedPrefs.getString("com.valensas.vlsync.lib.etag", null);
    }

    /**
     * @since 1.0
     *
     * @return Application Context stored at #mContext
     */
    protected Context getContext(){
        return this.mContext;
    }

    /**
     * Logging method used to debug sdk.
     *
     * @since 1.0
     *
     * @param message text to print console
     * @param throwable exception information
     */
    protected static void log(String message, Throwable throwable){
        if(mDebugEnabled){
            if(throwable == null){
                Log.d(TAG, message);
            }else {
                Log.d(TAG, message, throwable);
            }
        }
    }

    /**
     * Logging method used to debug sdk.
     *
     * @since 1.0
     *
     * @param message text to print console
     */
    protected static void log(String message){
        if(mDebugEnabled){
            Log.d(TAG, message, null);
        }
    }

    /**
     * @since 1.0
     *
     * @return Project Id from VLSync project on web.
     */
    protected String getId(){
        return this.mId;
    }

    /**
     * Listener for VLSync update process. It gives user the opportunity
     * of observing update states and take action in these states.
     *
     * @version 1.0
     * @since 1.0
     */
    public interface OnUpdateListener {

        /**
         * Called before the update process begins. It can be used to
         * start a progress bar, or give information to user about the
         * update process, etc.
         *
         * @since 1.0
         */
        public void onPreUpdate();

        /**
         * Called after the update process ends. It can be used to
         * re-organize UI to reflect changed content, etc.
         *
         * @since 1.0
         *
         * @param success indicates the status of update process
         * @param error if success is false, a VLSyncError is passed,
         *              otherwise null.
         */
        public void onPostUpdate(boolean success, VLSyncError error);

        /**
         * Called when the current progress changed. It can be used to
         * update progress bar's current value.
         *
         * @since 1.0
         *
         * @param progress Current progress between 0-100 inclusive
         */
        public void onProgressUpdate(int progress);
    }

    /**
     * Optional options can be passed to {@link #update} method or as
     * settings to VLSync instance.
     *
     * @version 1.0
     * @since 1.0
     */
    public enum UpdateOptionKey {

        /**
         * Progress state option key.
         *
         * @see com.valensas.vlsync.lib.VLSync.ProgressStyle
         * @since 1.0
         */
        PROGRESS_STYLE,

        /**
         * HUD state option key.
         *
         * @see com.valensas.vlsync.lib.VLSync.HUDState
         * @since 1.0
         */
        HUD_STATE
    }

    /**
     * Interface to unify different option states
     *
     * @version 1.0
     * @since 1.0
     */
    public interface UpdateOptionValue {

    }

    /**
     * HUD state values
     *
     * @version 1.0
     * @since 1.0
     */
    public enum HUDState implements UpdateOptionValue {

        /**
         * Indicates that a HUD is displayed when an update is in
         * progress.
         *
         * @since 1.0
         */
        VISIBLE,

        /**
         * Indicates that a HUD is not displayed when an update is in
         * progress.
         *
         * @since 1.0
         */
        HIDDEN
    }

    /**
     * Progress state values
     *
     * @version 1.0
     * @since 1.0
     */
    public enum ProgressStyle implements UpdateOptionValue {

        /**
         * Indicates that current progress is reflected to progress bar
         * and progress text is visible when an update is in progress.
         *
         * @since 1.0
         */
        DETERMINATE_TEXT_VISIBLE,

        /**
         * Indicates that a progress text is displayed with progress bar
         * and progress text is not visible when an update is in progress.
         *
         * @since 1.0
         */
        DETERMINATE_TEXT_HIDDEN,

        /**
         * Indicates that current progress is not reflected to progress bar
         * and progress text is not visible when an update is in progress.
         *
         * @since 1.0
         */
        INDETERMINATE_TEXT_HIDDEN,

        /**
         * Indicates that current progress is not reflected to progress bar
         * and progress text is visible when an update is in progress.
         *
         * @since 1.0
         */
        INDETERMINATE_TEXT_VISIBLE
    }
}
