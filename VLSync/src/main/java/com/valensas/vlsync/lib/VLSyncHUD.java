package com.valensas.vlsync.lib;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Custom dialog implementation for displaying HUD.
 * </br></br>
 * Created on 1/21/15</br>
 * Created @ Valensas
 *
 * @author Furkan Bayraktar
 * @version 1.0
 * @since 1.0
 */
class VLSyncHUD extends Dialog {

    /**
     * TextView reference for message.
     */
    private TextView mMessageText;

    /**
     * ProgressBar reference for progress.
     */
    private ProgressBar mProgressBar;

    /**
     * If this flag set true, progress is reflected to
     * progress bar. Otherwise, progress bar is indeterminate.
     */
    private boolean mShowProgress;

    /**
     * If this flag set true, progress is also shown in text
     * message.
     */
    private boolean mShowProgressText;

    /**
     * Constructor for VLSyncHUD
     *
     * @since 1.0
     *
     * @param context Context that HUD will be drawn.
     * @param theme Additional properties set from styles.xml.
     */
    private VLSyncHUD(Context context, int theme) {
        super(context, theme);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VLSync.log("Creating HUD...");
        setContentView(R.layout.progress_hud);
        setTitle("");

        mMessageText = (TextView) findViewById(R.id.message);
        mProgressBar = (ProgressBar) findViewById(R.id.progress);

        setCancelable(false);
        getWindow().getAttributes().gravity = Gravity.CENTER;
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.dimAmount=0.2f;
        getWindow().setAttributes(lp);

        if (mShowProgressText){
            mMessageText.setVisibility(View.VISIBLE);
        }else{
            mMessageText.setVisibility(View.GONE);
        }
        mProgressBar.setIndeterminate(!mShowProgress);
        VLSync.log("HUD created.");
    }

    /**
     * Updates progress bar's progress value and message.
     *
     * @since 1.0
     *
     * @param progress new progress value
     * @param message new progress message
     */
    protected void updateProgress(int progress, CharSequence message){
        if(mShowProgress) {
            if (progress >= 0 && progress <= 100) {
                mProgressBar.setProgress(progress);
            }
        }
        if(mShowProgressText){
            if (progress >= 0 && progress <= 100) {
                if (message != null && message.length() > 0) {
                    if(mShowProgress) {
                        mMessageText.setText(message + " " + progress + "%");
                    }else{
                        mMessageText.setText(message);
                    }
                }else{
                    if(mShowProgress) {
                        mMessageText.setText(progress + "%");
                    }else{
                        mMessageText.setText("");
                    }
                }
                mMessageText.invalidate();
            }
        }
    }

    /**
     * Update only progress bar's progress value.
     *
     * @since 1.0
     *
     * @param progress new progress value
     */
    protected void updateProgress(int progress){
        if(mShowProgress) {
            updateProgress(progress, null);
        }
    }

    /**
     * Create and show VLSyncHUD dialog instance with given options.
     *
     * @since 1.0
     *
     * @param context Context that HUD will be drawn.
     * @param showProgressText flag whether the #mMessageText is visible or not
     * @param showProgress flag whether the progress bar is indeterminate or not
     * @return shown VLSyncHUD dialog instance
     */
    protected static VLSyncHUD show(Context context, boolean showProgressText, boolean showProgress){
        VLSyncHUD dialog = new VLSyncHUD(context,R.style.VLSyncHUD);
        dialog.mShowProgress = showProgress;
        dialog.mShowProgressText = showProgressText;
        dialog.show();
        return dialog;
    }
}
