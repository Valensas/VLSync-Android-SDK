package com.valensas.vlsync.test;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.gson.Gson;
import com.valensas.vlsync.lib.VLSync;
import com.valensas.vlsync.lib.VLSyncError;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends ActionBarActivity implements VLSync.OnUpdateListener {

    /**
     * Debug tag.
     */
    private static final String TAG = "VLSyncTest";

    /**
     * VLSync instance.
     */
    private VLSync vlSync;

    /**
     * TextView used to display title.
     */
    private TextView title;

    /**
     * TextView used to display description.
     */
    private TextView description;

    /**
     * ImageView used to display image.
     */
    private ImageView image;

    /**
     * Single selection dialog used to change VLSync options.
     */
    private AlertDialog selectOptionsDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // UI initializations.

        setContentView(R.layout.activity_main);

        title = (TextView) findViewById(R.id.title);
        description = (TextView) findViewById(R.id.description);
        image = (ImageView) findViewById(R.id.image1);

        // VLSync SDK initialization method call.

        initializeVLSync();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get current time
        Date dt = new Date();

        // Check last update time. Every 30 minutes update
        // if needed. It is not necessary because if there is
        // no change, it will not download anything.
        if(dt.getTime() - vlSync.lastUpdate() >= 1800000) {
            vlSync.update(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == R.id.options){
            if(selectOptionsDialog == null){
                createOptionsDialog();
            }
            selectOptionsDialog.show();
            return true;
        }else if(item.getItemId() == R.id.update){
            // Start update operation. If you do not want to
            // display process indicator (aka HUD), you do not
            // need to pass context to update method.
            vlSync.update(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * This method used to create an options dialog to
     * demonstrate different types of options can be
     * applied to VLSync.
     */
    private void createOptionsDialog(){
        final String[] options = {"HUD Visible","HUD Hidden","Determinate Progress", "Indeterminate Progress", "Progress Text"};

        AlertDialog.Builder optionBuilder = new AlertDialog.Builder(this);
        optionBuilder.setTitle("Select option")
                .setItems(options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        // Create a map object to store options. Fill the map according to
                        // selected option. Options are not necessary, but useful for many cases.
                        // You can also create your own flow by using callback methods provided
                        // by OnUpdateListener.
                        Map<VLSync.UpdateOptionKey, VLSync.UpdateOptionValue> options = new HashMap<VLSync.UpdateOptionKey, VLSync.UpdateOptionValue>();
                        switch (which){
                            case 0:
                                options.put(VLSync.UpdateOptionKey.HUD_STATE, VLSync.HUDState.VISIBLE);
                                break;
                            case 1:
                                options.put(VLSync.UpdateOptionKey.HUD_STATE, VLSync.HUDState.HIDDEN);
                                break;
                            case 2:
                                options.put(VLSync.UpdateOptionKey.HUD_STATE, VLSync.HUDState.VISIBLE);
                                options.put(VLSync.UpdateOptionKey.PROGRESS_STYLE, VLSync.ProgressStyle.DETERMINATE_TEXT_HIDDEN);
                                break;
                            case 3:
                                options.put(VLSync.UpdateOptionKey.HUD_STATE, VLSync.HUDState.VISIBLE);
                                options.put(VLSync.UpdateOptionKey.PROGRESS_STYLE, VLSync.ProgressStyle.INDETERMINATE_TEXT_HIDDEN);
                                break;
                            case 4:
                                options.put(VLSync.UpdateOptionKey.HUD_STATE, VLSync.HUDState.VISIBLE);
                                options.put(VLSync.UpdateOptionKey.PROGRESS_STYLE, VLSync.ProgressStyle.DETERMINATE_TEXT_VISIBLE);
                                break;
                        }
                        // Set options. When the options are set, they become default
                        // options for update operations. You can also set temporary
                        // options by calling update method with options map.
                        vlSync.setUpdateOptions(options);

                        // Start update operation. If you do not want to
                        // display process indicator (aka HUD), you do not
                        // need to pass context to update method.
                        vlSync.update(MainActivity.this);
                    }
                });

        selectOptionsDialog = optionBuilder.create();
    }

    /**
     * Initialization method for VLSync SDK. SDK must be initialized
     * with project id and application context. Project Id can be
     * obtained from VLSync Portal under the Settings menu.
     */
    private void initializeVLSync(){
        // Initialize SDK for usage. If you want to use SDK in any
        // other places in your project, you do not need to initialize
        // SDK. You can just use getInstance method of VLSync class
        // to use it.
        vlSync = VLSync.initWithProjectId("df22a3aa-24ef-48fc-a4a6-0bcbdf70943e", getApplicationContext());

        // If debugging set enabled, every step inside the SDK
        // will be printed to console. It is useful to debug
        // your project but recommended to be disabled when you
        // release your application.
        vlSync.setDebugEnabled(true);

        // Set on update listener to receive callbacks from SDK.
        // It is not necessary to be set.
        vlSync.setOnUpdateListener(this);
    }

    /**
     * Refresh view to display updated data. Called after
     * update operation. In demo project, we have a JSON file on
     * VLSync server which contains 3 fields, title, description,
     * and image. In this method, we first parse JSON to object and
     * fill UI according to data.
     */
    private void refreshView(){
        try {
            // Parse JSON
            File file = new File(vlSync.getRootFolder(), "main.json");
            Gson gson = new Gson();
            BufferedReader br = new BufferedReader(new FileReader(file));
            MyObject object = gson.fromJson(br, MyObject.class);

            // Set title and description
            title.setText(object.title);
            description.setText(object.description);

            // Get image from storage and display.
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(new File(vlSync.getRootFolder(), object.image).getAbsolutePath(), options);
            image.setImageBitmap(bitmap);

        }catch (Exception ex){
            // If an error occurs, print error.
            ex.printStackTrace();
        }
    }

    @Override
    public void onPreUpdate() {
        // Called just before the update operation.
        Log.d(TAG, "Update process is starting...");
    }

    @Override
    public void onPostUpdate(boolean success, VLSyncError error) {
        // Called just after the update operation. If success is true,
        // update operation is successful and error is null. Otherwise,
        // error object contains a message about the failed operation.
        if(success) {
            refreshView();
        }else{
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Error!");
            builder.setMessage(error.getMessage());
            builder.setPositiveButton("Dismiss", null);
            builder.show();
        }
    }

    @Override
    public void onProgressUpdate(int progress) {
        // When update progress changes, this method is called
        // with new progress value.
        Log.d(TAG, "Progress received: " + progress);
    }

    /**
     * POJO for demo project. In demo project, we stored a JSON file
     * in VLSync server called main.json. This class represents the
     * object stored in json.
     */
    private class MyObject {
        String title;
        String description;
        String image;
    }
}