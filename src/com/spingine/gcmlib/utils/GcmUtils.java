package com.spingine.gcmlib.utils;

import java.io.IOException;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.util.Log;

public final class GcmUtils {
	
	private final static String TAG = GcmUtils.class.getSimpleName();
	
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	
	private final static String SERVER_URL = "";

	private static final String PROPERTY_REG_ID = "registration_id";

	private static final String PROPERTY_APP_VERSION = "application_version";
	
	private static GcmUtils mInstance = null;
	
	private Context mContext = null;
	
	private GoogleCloudMessaging gcm = null;
	
	private String SENDER_ID = null;
	
	private String regid = null;
	
	private Class<Activity> mClassUsed = null;
	
	
	/**
	 * the listener in which registration id finishes
	 * @author Ali Montecillo
	 *
	 */
	private FinishAsyncTaskListener onFinishTask = null;
	public interface FinishAsyncTaskListener{
		void onFinishCall(String result);
	}
	
	
	private RegisterCallback onRegister = null;
	public interface RegisterCallback {
		void onRegisterStart();
		void onRegisterStop();
	}
	
	public static GcmUtils getInstance(Context context , Class<Activity> cls) throws Exception{
		
		if(!(context instanceof Activity))
			throw new Exception("Error : not possible!");
		
		if(mInstance == null){
			mInstance = new GcmUtils(context , cls);
		}
		
		
		
		return mInstance;
	}
	
	public GcmUtils(Context context , Class<Activity> cls){
		this.mContext = context;
		this.mClassUsed = cls;
	}
	
	/**
	 * check whether there is already an existing regid or not
	 * @return
	 */
	public boolean hasRegid(){
		final SharedPreferences shared = getGCMPreferences(mContext);
		
		if(shared.contains(PROPERTY_REG_ID)){
			return true;
		}
		return false;
	}
	
	/**
	 * background registration
	 */
	public void doRegister(){
		registerInBackground();
	}
	
	
	/**
	 * Check the device to make sure it has the Google Play Services APK. If
	 * it doesn't, display a dialog that allows users to download the APK from
	 * the Google Play Store or enable it in the device's system settings.
	 */
	private boolean checkPlayServices() {
	    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext);
	    if (resultCode != ConnectionResult.SUCCESS) {
	        if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
	            GooglePlayServicesUtil.getErrorDialog(resultCode, (Activity) mContext,
	                    PLAY_SERVICES_RESOLUTION_REQUEST).show();
	        } else {
	            Log.i(TAG, "This device is not supported.");
	            ((Activity) mContext).finish();
	        }
	        return false;
	    }
	    return true;
	}
	
	
	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration ID and app versionCode in the application's
	 * shared preferences.
	 */
	private void registerInBackground() {
	    new AsyncTask<Void, Void, String>() {
	        @Override
	        protected String doInBackground(Void... params) {
	        	/*
	        	 * callback to listen for registration...
	        	 */
	    		if(onRegister != null){
	    			onRegister.onRegisterStart();
	    		}
	            String msg = "";
	            
	            try {
	                if (gcm == null) {
	                    gcm = GoogleCloudMessaging.getInstance(mContext);
	                }
	                regid = gcm.register(SENDER_ID);
	                msg = "success";

	                // You should send the registration ID to your server over HTTP,
	                // so it can use GCM/HTTP or CCS to send messages to your app.
	                // The request to your server should be authenticated if your app
	                // is using accounts.
	                sendRegistrationIdToBackend();

	                // For this demo: we don't need to send it because the device
	                // will send upstream messages to a server that echo back the
	                // message using the 'from' address in the message.

	                // Persist the regID - no need to register again.
	                storeRegistrationId(mContext, regid);
	            } catch (IOException ex) {
	                msg = "Error :" + ex.getMessage();
	                // If there is an error, don't just keep trying to register.
	                // Require the user to click a button again, or perform
	                // exponential back-off.
	            }
	            return msg;
	        }

	        @Override
	        protected void onPostExecute(String msg) {
	            Log.d(TAG, msg);
	            if(regid != null && !regid.equals("")){
	            	if(onFinishTask != null){
	            		onFinishTask.onFinishCall(msg);
	            	}
	            	
	            	if(onRegister != null){
	            		onRegister.onRegisterStop();
	            	}
	            }
	        }


	    }.execute(null, null, null);
	    
	}
	/**!!IMPORTANT
	 * send and store registration to dynamodb
	 * using the server_url
	 */
	private void sendRegistrationIdToBackend(){
		
	}
	
	/**
	 * store registration id in local sql file 
	 * using device storage
	 * @param context
	 * @param regid
	 */
	private void storeRegistrationId(Context context, String regid){
	    final SharedPreferences prefs = getGCMPreferences(context);
	    int appVersion = getAppVersion(context);
	    Log.i(TAG, "Saving regId on app version " + appVersion);
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString(PROPERTY_REG_ID, regid);
	    editor.putInt(PROPERTY_APP_VERSION, appVersion);
	    editor.commit();
	}
	
	/**
	 * @return Application's {@code SharedPreferences}.
	 */
	private SharedPreferences getGCMPreferences(Context context) {
	    // This sample app persists the registration ID in shared preferences, but
	    // how you store the regID in your app is up to you.
	    return mContext.getSharedPreferences(mClassUsed.getSimpleName(),
	            Context.MODE_PRIVATE);
	}
	
	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	private static int getAppVersion(Context context) {
	    try {
	        PackageInfo packageInfo = context.getPackageManager()
	                .getPackageInfo(context.getPackageName(), 0);
	        return packageInfo.versionCode;
	    } catch (NameNotFoundException e) {
	        // should never happen
	        throw new RuntimeException("Could not get package name: " + e);
	    }
	}

	/**
	 * @return the mContext
	 */
	public Context getmContext() {
		return mContext;
	}

	/**
	 * @param mContext the mContext to set
	 */
	public void setmContext(Context mContext) {
		this.mContext = mContext;
	}

	/**
	 * @return the sENDER_ID
	 */
	public String getSENDER_ID() {
		return SENDER_ID;
	}

	/**
	 * @param sENDER_ID the sENDER_ID to set
	 */
	public void setSENDER_ID(String sENDER_ID) {
		SENDER_ID = sENDER_ID;
	}

	/**
	 * @return the mClassUsed
	 */
	public Class<Activity> getmClassUsed() {
		return mClassUsed;
	}

	/**
	 * @param mClassUsed the mClassUsed to set
	 */
	public void setmClassUsed(Class<Activity> mClassUsed) {
		this.mClassUsed = mClassUsed;
	}

	/**
	 * @return the onFinishTask
	 */
	public FinishAsyncTaskListener getOnFinishTask() {
		return onFinishTask;
	}

	/**
	 * @param onFinishTask the onFinishTask to set
	 */
	public void setOnFinishTask(FinishAsyncTaskListener onFinishTask) {
		this.onFinishTask = onFinishTask;
	}

	/**
	 * @return the serverUrl
	 */
	public static String getServerUrl() {
		return SERVER_URL;
	}

	/**
	 * @return the regid
	 */
	public String getRegid() {
		return regid;
	}
	
	
	
	
}
