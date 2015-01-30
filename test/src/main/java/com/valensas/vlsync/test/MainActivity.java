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
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends ActionBarActivity implements VLSync.OnUpdateListener {

    private static final String TAG = "VLSyncTest";

    private VLSync vlSync;

    private TextView title;
    private TextView description;
    private ImageView image1;

    private AlertDialog selectOptionsDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        title = (TextView) findViewById(R.id.title);
        description = (TextView) findViewById(R.id.description);
        image1 = (ImageView) findViewById(R.id.image1);

        initializeVLSync();
    }

    @Override
    protected void onResume() {
        super.onResume();
        vlSync.update(this);
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
            vlSync.update(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void createOptionsDialog(){
        final String[] options = {"HUD Visible","HUD Hidden","Determinate Progress", "Indeterminate Progress", "Progress Text"};

        AlertDialog.Builder optionBuilder = new AlertDialog.Builder(this);
        optionBuilder.setTitle("Select option")
                .setItems(options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
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
                        vlSync.setUpdateOptions(options);
                        vlSync.update(MainActivity.this);
                    }
                });

        selectOptionsDialog = optionBuilder.create();
    }

    private void initializeVLSync(){
        vlSync = VLSync.initWithProjectId("df22a3aa-24ef-48fc-a4a6-0bcbdf70943e", getApplicationContext());
        vlSync.setDebugEnabled(true);
        vlSync.setOnUpdateListener(this);
    }

    private void refreshView(){
        try {
            File file = new File(vlSync.getRootFolder(), "main.json");
            Gson gson = new Gson();
            BufferedReader br = new BufferedReader(new FileReader(file));
            MyObject object = gson.fromJson(br, MyObject.class);

            title.setText(object.title);
            description.setText(object.description);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(new File(vlSync.getRootFolder(), object.image).getAbsolutePath(), options);
            image1.setImageBitmap(bitmap);

        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void onPreUpdate() {
        Log.d(TAG, "Update process is starting...");
    }

    @Override
    public void onPostUpdate(boolean success, VLSyncError error) {
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
        Log.d(TAG, "Progress received: " + progress);
    }

    private class MyObject {
        String title;
        String description;
        String image;
    }
}