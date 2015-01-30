## VLSync Android SDK
###Overview
VLSync SDK for Android is a library to interact with files stored at VLSync server. In order to use VLSync SDK, you must have a project at [vlsync.valensas.com](http://vlsync.valensas.com). 

VLSync SDK for Android supports all versions of Android above 2.3 which corresponds to SDK version 9 and above. 

###Installation

Download and import the library module in Android Studio or IntelliJ IDEA. Add dependecy to your build.gradle file.

	compile project(':VLSync')

###Usage and Demo

A working demo is located under test folder.  Documentation of all classes are located under docs folder. 

In order to use, library must be initialized by project's folder name obtained from settings page in [VLSync Portal](http://vlsync.valensas.com).  The following method is used in demo project to initialize VLSync SDK. 

	private void initializeVLSync(){
        vlSync = VLSync.initWithProjectId("xxxxx", getApplicationContext());
        vlSync.setDebugEnabled(true);
        vlSync.setOnUpdateListener(this);
    }
   
To receive different VLSync actions, the activity object in this case implemented VLSync.OnUpdateListener interface with the following methods:

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

As seen above, additional functions can be used to extend library's abilities. Available functions are:

	public void setUpdateOptions(Map<UpdateOptionKey, UpdateOptionValue>)
	public int progress()
	public long lastUpdate()
	public File getRootFolder()
	public void setOnUpdateListener(OnUpdateListener listener)
	public void setDebugEnabled(boolean enabled)

This functions can be used by developer to use VLSync more efficiently in their projects. The demo project uses all of the methods above in different places. 

###Changelog

 - 1.0 (30.01.2015)
	 - Initial version.