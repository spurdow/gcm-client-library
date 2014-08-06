gcm-client-library
==================

this client library thinks that you already know the basics of GCM.

If you do! proceed!


*just same old style with adding service and receiver in manifest.

*Remember to add the ExtendedService and ExtendedReceiver in the AndroidManifest, NOT the abstract classes but the implementing classes itself.

*extend the abstract service, and the abstract receiver 

Implementation of AbstractService

````java

public class ExampleExtensionGcmIntentService extends GcmIntentService {

	 
	@Override
	public void sendNotification(Bundle extras) {
		// TODO Auto-generated method stub
		// use the bundle extra to get the received message here
		extras.getString("your-key-here");
		// use it to notify or what your app do
	}

}
````

Implementation of AbstractReceiver

````java

public class ExampleExtensionGcmBroadcastReceiver extends GcmBroadcastReceiver {

	@Override
	public ComponentName getComponentFromChild(Context context) {
		// TODO Auto-generated method stubConte
		return new ComponentName(context.getPackageName(),
                ExampleExtensionGcmIntentService.class.getName());
		
	}


}
````

// this means register it to your server, use php or whatever you are comfortable with.
*extend the abstract GcmUtils implement one of the important methods registrationIdToBackground();

*after implementing your own GcmUtils setSender Id and url..

Implementation of abstract GcmUtils

````java

public class ExampleExtensionGcmUtils extends GcmUtils {

	public ExampleExtensionGcmUtils(Context context, Class cls) {
		super(context, cls);
		// TODO Auto-generated constructor stub
		setSENDER_ID("your-sender-id-here");
	}

	@Override
	public void sendRegistrationIdToBackend(HttpEntity entity,
			HttpResponse response) {
		// TODO Auto-generated method stub
		// your implementation here when registering id to your backend

		response = makeHttpRequest(the-context, the-name-value-pair-list , URL);
		entity = response.getEntity();
		// do something about entity
	}

	private HttpResponse makeHttpRequest(Context c, List<NameValuePair> lits, String url){
		// your code here to send request to server
	}

}
````
how to use in activity

````java
public class GcmActivityTest extends Activity{

	protected ExampleExtensionGcmUtils gcm;
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		gcm = new ExampleExtensionGcmUtils(this , GcmActivityTest.class);
		// if we arent registered
		// call do register
		if(!gcm.hasRegid()){
			gcm.doRegister();
		}else{
			//use the regid
			final String regid = gcm.getRegid();
		}
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if(!gcm.checkPlayServices()){
			// we dont have play services in this phone
			// disable the app or any workaround for the user
		}
	}

	
}
````

````xml
    <!--  GCM permissions -->
    <uses-permission android:name="android.permission.GET_ACCOUNTS" />
    <uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />
    <permission android:name="com.example.gcm.permission.C2D_MESSAGE"
        android:protectionLevel="signature" />
    <uses-permission android:name="com.example.gcm.permission.C2D_MESSAGE" />
    <!-- Receiver -->
    <receiver
        android:name="com.your-package.ExampleExtensionGcmBroadcastReceiver"
        android:permission="com.google.android.c2dm.permission.SEND" >
            <intent-filter>
                <action android:name="com.google.android.c2dm.intent.RECEIVE" />
                <category android:name="com.your-package" />
            </intent-filter>
    </receiver>

    <!-- Service -->
    <service android:name="com.your-package.ExampleExtensioGcmIntentService" />


````

*and your good to go!

