/*
 * Downloading bin files using WCFService
 */

package thermapp.sdk.sample;

import java.net.HttpURLConnection;
import java.net.URL;

import thermapp.sdk.DeviceData_Callback;
import thermapp.sdk.ThermAppAPI;

import com.google.analytics.tracking.android.EasyTracker;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
* Welcome Activity Object.
* 
* <P>Initial activity that takes care of:
* <ul>
*   <li>Terms of use</li>
*   <li>Testing Internet connection</li>
*   <li>Downloading of calibration tables</li>
* </ul> 
*/
public class WelcomeActivity extends Activity implements DeviceData_Callback 
{

	private ProgressBar mProgressBar;
	private TextView mProgressText;
	private TextView mSerial;
	private String serialnum = "00000000";
	private LinearLayout mDownloadLayout;
	private LinearLayout mRetryLayout;
	private LinearLayout mTermsLayout;
	private Button mRetryButton;
	private Button mCloseButton;
	private SharedPreferences mPrefs;
	private Button mTermsButton;
	private Button mNotAcceptButton;
	private ImageButton mAcceptButton;
	private TextView mInitLabel;
	private TextView mRetryText;
	
	@Override
	public void onStart() 
	{
		super.onStart();

		EasyTracker.getInstance(this).activityStart(this); 
	}

	@Override
	public void onStop() 
	{
		super.onStop();

		EasyTracker.getInstance(this).activityStop(this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcome);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Get app preferences
		mPrefs = PreferenceManager.getDefaultSharedPreferences(WelcomeActivity.this);
		
		// Get controls instances
		mDownloadLayout  = (LinearLayout) findViewById(R.id.donwload_lay);
		mRetryLayout     = (LinearLayout) findViewById(R.id.retry_lay);
		mTermsLayout     = (LinearLayout) findViewById(R.id.terms_lay);
		mRetryButton     = (Button) findViewById(R.id.retry_button);
		mCloseButton     = (Button) findViewById(R.id.close_button);
		mTermsButton     = (Button) findViewById(R.id.button_tofu);
		mAcceptButton    = (ImageButton) findViewById(R.id.imageButton_accept);
		mNotAcceptButton = (Button) findViewById(R.id.button_not_accept);
		mInitLabel		 = (TextView) findViewById(R.id.textView_rec);
		mRetryText       = (TextView) findViewById(R.id.textViewRetry);

		// Setup buttons listeners
		mTermsButton.setOnTouchListener(new ButtonHighlighterOnTouchListener(mTermsButton));
		mAcceptButton.setOnTouchListener(new ButtonHighlighterOnTouchListener(mAcceptButton));
		mNotAcceptButton.setOnTouchListener(new ButtonHighlighterOnTouchListener(mNotAcceptButton));

		mTermsButton.setOnClickListener(new View.OnClickListener() 
		{

			@Override
			public void onClick(View v) 
			{

				Intent in = new Intent(WelcomeActivity.this, TermsOfUse.class);
				startActivity(in);

			}
		});

		mAcceptButton.setOnClickListener(new View.OnClickListener() 
		{

			@Override
			public void onClick(View v) 
			{

				mPrefs.edit().putBoolean("terms_shown", true).commit();
				checkDeviceData();
			}
		});

		mNotAcceptButton.setOnClickListener(new View.OnClickListener() 
		{

			@Override
			public void onClick(View v) 
			{

				Intent returnIntent = new Intent();
				returnIntent.putExtra("result", "EXIT");
				setResult(RESULT_OK, returnIntent);
				finish();
			}
		});

		mRetryButton.setOnClickListener(new View.OnClickListener() 
		{

			@Override
			public void onClick(View v) 
			{
				checkDeviceData();
			}
		});
		
		mCloseButton.setOnClickListener(new View.OnClickListener() 
		{

			@Override
			public void onClick(View v) 
			{
				Intent returnIntent = new Intent();
				returnIntent.putExtra("result", "EXIT");
				setResult(RESULT_CANCELED, returnIntent);
				finish();
			}
		});

		mProgressBar = (ProgressBar) findViewById(R.id.progressBar_download);
		mProgressText = (TextView) findViewById(R.id.textView_perc);
		
		// Get serial number from main activity
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			serialnum = extras.getString("serialnum");
		}
		
		// Display serial number
		mSerial = (TextView) findViewById(R.id.textView_initializing);
		mSerial.setText("Serial #: " + serialnum);
		
		// Check if we need to confirm terms of use  
		if (!mPrefs.getBoolean("terms_shown", false)) 
		{
			mDownloadLayout.setVisibility(View.GONE);
			mRetryLayout.setVisibility(View.GONE);
			return;
		}

		// If all goes well, check if we have the device data locally
		checkDeviceData();
	}

	/**
	 * Checks if the device has all needed files to operate
	 */
	protected void checkDeviceData() 
	{

		// Hide terms
		mTermsLayout.setVisibility(View.GONE);
		mRetryLayout.setVisibility(View.GONE);
		
		// Show download layout
		mDownloadLayout.setVisibility(View.VISIBLE);

		// Do we have everything we need?
		if(ThermAppAPI.CheckDeviceData()) 
		{
			OnDownloadFinished();
			return;
		}

		// Start Check Internet task 
		new CheckInternetTask(this).execute();		
	}
			
	@Override
	public void OnDownloadFinished() 
	{
		Intent returnIntent = new Intent();
		returnIntent.putExtra("result", "OK");
		setResult(RESULT_OK, returnIntent);
		finish();
	}

	@Override
	public void OnError(String errorMsg) 
	{
		
		if (errorMsg.compareTo("DownloadFailedBadSN") == 0)
		{
			// SN doesn't exists or inactive
			String sErrorMsg = getResources().getString(R.string.welcome_retry_text_unrecognized_begin) + " " + serialnum + " " + getResources().getString(R.string.welcome_retry_text_unrecognized_end);
			mRetryText.setText(sErrorMsg);
		}
		else
			mRetryText.setText(R.string.welcome_retry_text);
		
		// handle network disconnected (show retry)
		mRetryLayout.setVisibility(View.VISIBLE);
		mDownloadLayout.setVisibility(View.GONE);
		mProgressBar.setProgress(0);
		mProgressText.setText("Please wait...");
		mInitLabel.setText(R.string.welcome_download_text_upper);
	}

	@Override
	public void OnUpdateProgress(String sProgressText, int value)
	{
		mInitLabel.setText(getResources().getString(R.string.welcome_download_text_upper_downloading) + " " + sProgressText);
		mProgressBar.setProgress(value);
		mProgressText.setText("" + value + "%");
	}

	/**
	 * Task for CheckInternet process
	 * This will try to get a response from a google server using the generate_204 method
	 */
    private class CheckInternetTask extends AsyncTask<String, Void, Boolean> 
    {

    	WelcomeActivity activity;
    	
        public CheckInternetTask(WelcomeActivity welcomeActivity) 
        {
        	activity = welcomeActivity;
		}

		@Override
        protected Boolean doInBackground(String... params) 
        {
 
    		try {
    			ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    			NetworkInfo netInfo = cm.getActiveNetworkInfo();
    			if (netInfo == null)
    			{
    				Log.e("Connection Info", "Error checking internet connection");
    				return false;
    			}
    		    if (netInfo.isConnected()) 
    		    {
    	            HttpURLConnection urlc = (HttpURLConnection) 
    	                (new URL("http://clients3.google.com/generate_204")
    	                .openConnection());
    	            urlc.setRequestProperty("User-Agent", "Android");
    	            urlc.setRequestProperty("Connection", "close");
    	            urlc.setConnectTimeout(1500); 
    	            urlc.connect();
    	            return (urlc.getResponseCode() == 204 && urlc.getContentLength() == 0);
    		    } 
    		    else
    		        Log.d("Connection Info", "No network available!");
    		}
    		catch (Exception e)
    		{
    			Log.e("Connection Info", "Error checking internet connection", e);
    			return false;
    		}
    	    return false;
        }

        @Override
        protected void onPostExecute(Boolean result) 
        {
    		// handle network disconnected (show retry)
    		if (!result) {
    			// Show no Internet message
    			mRetryText.setText(R.string.welcome_retry_text_no_internet);
    			mRetryLayout.setVisibility(View.VISIBLE);
    			mDownloadLayout.setVisibility(View.GONE);
    			return;
    		}

    		// Else - start downloading
    		mInitLabel.setText(R.string.welcome_download_text_upper_downloading);
    		try {
				ThermAppAPI.DownloadDeviceData(activity);
			} catch (Exception e) 
			{
				e.printStackTrace();
			}
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }
    
}
