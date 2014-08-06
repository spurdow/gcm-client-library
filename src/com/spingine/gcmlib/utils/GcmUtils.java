package com.spingine.gcmlib.utils;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;

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

public abstract class GcmUtils {
	
	private final static String TAG = GcmUtils.class.getSimpleName();
	
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	
	protected String SERVER_URL = "";

	private static final String PROPERTY_REG_ID = "registration_id";

	private static final String PROPERTY_APP_VERSION = "application_version";
	
	
	protected Context mContext = null;
	
	protected GoogleCloudMessaging gcm = null;
	
	protected String SENDER_ID = null;
	
	protected String regid = null;
	
	private Class mClassUsed = null;
	
	
	/**
	 * the listener in which registration id finishes
	 * @author Ali Montecillo
	 *
	 */
	private FinishAsyncTaskListener onFinishTask = null;
	public interface FinishAsyncTaskListener{
		void onFinishCallSuccess(String result);
		void onFinishCallError(String result);
	}
	
	
	private RegisterCallback onRegister = null;
	public interface RegisterCallback {
		void onRegisterStart();
		void onRegisterStopSuccess();
		void onRegisterStopError();
	}
	
	public GcmUtils(Context context , Class cls){
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
	public boolean checkPlayServices() {
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
	    		HttpEntity entity = null;
	            HttpResponse response = null;
	    		String msg = "";
	            
	            try {
	                if (gcm == null) {
	                    gcm = GoogleCloudMessaging.getInstance(mContext);
	                }
	                regid = gcm.register(SENDER_ID);
	                msg = "";
	                

	                // You should send the registration ID to your server over HTTP,
	                // so it can use GCM/HTTP or CCS to send messages to your app.
	                // The request to your server should be authenticated if your app
	                // is using accounts.
	                sendRegistrationIdToBackend(entity , response);

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
	            if(response != null && entity != null){
		            final int statCode = response.getStatusLine().getStatusCode();
		            if(statCode == HttpStatus.SC_OK){
		            	try {
							msg = EntityUtils.toString(entity);
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} finally{
							if(response != null){
								response = null;
							}
							if(entity != null){
								try {
									entity.consumeContent();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}finally{
									entity = null;
								}
							}
						}
		            }else{
		            	msg = statCode+"";
		            }
	            }
	            Log.d(TAG, "Message = " + msg);
	            return msg;
	        }
	        
	        

	        /* (non-Javadoc)
			 * @see android.os.AsyncTask#onProgressUpdate(java.lang.Object[])
			 */
			@Override
			protected void onProgressUpdate(Void... values) {
				// TODO Auto-generated method stub
				super.onProgressUpdate(values);
			}



			@Override
	        protected void onPostExecute(String msg) {
	            Log.v("PostExecute", msg);
	            if(regid != null && !regid.equals("") && !isNumber(msg)){
	               	if(onFinishTask != null){
	            		onFinishTask.onFinishCallSuccess(msg);
	            	}
	            	if(onRegister != null){
	            		onRegister.onRegisterStopSuccess();
	            	}
	            }else{
	            	
	               	if(onFinishTask != null){
	            		onFinishTask.onFinishCallError(msg);
	               	}
	            	if(onRegister != null){
	            		onRegister.onRegisterStopError();
	            	}
	            }
	            
	            
 
            	

	        }


	    }.execute(null, null, null);
	    
	}
	/**!!IMPORTANT
	 * send and store registration to your database
	 * using the server_url
	 */
	public abstract void sendRegistrationIdToBackend(HttpEntity entity , HttpResponse response);
	
	
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
	public Class getmClassUsed() {
		return mClassUsed;
	}

	/**
	 * @param mClassUsed the mClassUsed to set
	 */
	public void setmClassUsed(Class mClassUsed) {
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
	 * @return the onRegister
	 */
	public RegisterCallback getOnRegister() {
		return onRegister;
	}

	/**
	 * @param onRegister the onRegister to set
	 */
	public void setOnRegister(RegisterCallback onRegister) {
		this.onRegister = onRegister;
	}

	/**
	 * @return the regid
	 */
	public String getRegid() {
		SharedPreferences shared = getGCMPreferences(mContext);
		if(hasRegid()){
			regid = shared.getString(PROPERTY_REG_ID, "");
		}
		
		return regid;
	}
	
	public  boolean isNumber(String n) {
		// TODO Auto-generated method stub
		return n != null && n.matches("[0-9]+") && n.length() > 0;

	}
	
	
}
