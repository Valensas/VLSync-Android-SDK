package com.valensas.vlsync.test;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
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


public class MainActivity extends ActionBarActivity {

    private VLSync vlSync;

    private TextView title;
    private TextView description;
    private ImageView image1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vlSync = VLSync.initWithProjectId("df22a3aa-24ef-48fc-a4a6-0bcbdf70943e", getApplicationContext());

        Map<VLSync.UpdateOptionKey, VLSync.UpdateOptionValue> options = new HashMap<VLSync.UpdateOptionKey, VLSync.UpdateOptionValue>();
        options.put(VLSync.UpdateOptionKey.HUD_STATE, VLSync.HUDState.VISIBLE);
        options.put(VLSync.UpdateOptionKey.PROGRESS_STYLE, VLSync.ProgressStyle.DETERMINATE_TEXT_VISIBLE);

        vlSync.setUpdateOptions(options);
        vlSync.setDebugEnabled(true);

        vlSync.setOnUpdateListener(new VLSync.OnUpdateListener() {
            @Override
            public void onPreUpdate() {

            }

            @Override
            public void onPostUpdate(boolean success, VLSyncError error) {
                initializeView();
            }

            @Override
            public void onProgressUpdate(int progress) {

            }
        });
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

        if(item.getItemId() == R.id.update){
            vlSync.update(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initializeView(){
        title = (TextView) findViewById(R.id.title);
        description = (TextView) findViewById(R.id.description);
        image1 = (ImageView) findViewById(R.id.image1);

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

    private class MyObject {
        String title;
        String description;
        String image;
    }
}
