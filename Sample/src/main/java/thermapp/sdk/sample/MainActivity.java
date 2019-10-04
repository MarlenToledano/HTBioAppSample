package thermapp.sdk.sample;

import java.io.File;
import java.text.DecimalFormat;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;

import thermapp.sdk.MeasurementData;
import thermapp.sdk.ThermAppAPI;
import thermapp.sdk.ThermAppAPI_Callback;

@TargetApi(23)
public class MainActivity extends Activity implements ThermAppAPI_Callback
{
	private static final int 	PHONE_STROGE_PERMISSION = 100;

	private ThermAppAPI mDeviceSdk = null;
	private Bitmap bmp_ptr = null;

	int[] gray_palette;
	int[] therm_palette;
	int[] my_palette;
	int[] mRevGrayPalette;

	Matrix matrix_imrot_90;



	private Runnable rnbl = new Runnable()
	{
		public void run()
		{
			iv.setImageBitmap(Bitmap.createBitmap(bmp_ptr, 0, 0,
					bmp_ptr.getWidth(), bmp_ptr.getHeight(), matrix_imrot_90,
					true));
		}
	};
	private static final String media_path = Environment
			.getExternalStorageDirectory().getPath() + "/ThermApp/Media";

	private TextView textView_temp = null;
	private TextView textView_minTemp = null;
	private TextView textView_maxTemp = null;
	private TextView textView_linkToThermAppWebsite = null;
	private ImageView iv = null;
	private ImageButton button_settings = null;

	private float[] CtoF;
	private String FerCel;

	private SharedPreferences prefs;
	private RelativeLayout rLayout_tempView;
	private ImageView imageView_colorsPane;
	private RelativeLayout rLayout_initializationScreen;
	private RelativeLayout rLayout_noCameraScreen;
	private ImageView barBck;

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
	public void OnFrameGetThermAppBMP(Bitmap bmp, int[] iMinMaxThresholdValues, Bitmap[] allBitmap)
	{
		if (null != bmp)
		{
			bmp_ptr = bmp;
			iv.post(rnbl);
		}
		
		/*
		 * Every callback the minimum and maximum temperatures in the frame are received.
		 * Temperatures units are integers but treated as floats – therefore their values should be [Temperature] * 100.
		 * For example, 25.70 degrees is received as 2570.*/
		final float min_temperature = (float)(iMinMaxThresholdValues[0]);
		final float max_temperature = (float)(iMinMaxThresholdValues[1]);
/*
		int threshold = iMinMaxThresholdValues[2];	// Grayscale switching point between B&W and color, range is [0-255].
*/
		textView_minTemp.post(new Runnable()
		{
			public void run()
			{
				DecimalFormat form = new DecimalFormat("00°" + FerCel);
				textView_minTemp.setText(form.format(min_temperature/100));
			}
		});

		textView_maxTemp.post(new Runnable()
		{
			public void run()
			{
				DecimalFormat form = new DecimalFormat("00°" + FerCel);
				textView_maxTemp.setText(form.format(max_temperature/100));
			}
		});

	}

	@Override
	public void OnFrameGetThermAppTemperatures(final int[] frame, final int w, final int h, MeasurementData measurementData)
	{
		final float central_pix = (float) (frame[(w >> 1) * (h + 1)]) / 100
				* CtoF[0] + CtoF[1];

		textView_temp.post(new Runnable()
		{
			public void run()
			{
				DecimalFormat form = new DecimalFormat("#,##00.0°" + FerCel);
				textView_temp.setText(form.format(central_pix));
			}
		});

		/*
		 * On ThermApp-TH devices, when measurement mode is set to AREA/LINE/HILO, the following data fields contain:
		 * 	measurementData.fMin	- minimum temperature at the current measurement mode
		 *	measurementData.fMax	- maximum temperature at the current measurement mode 
		 *	measurementData.fAvg	- average temperature at the current measurement mode 
		 *	measurementData.pMin	- IR coordinate of the minimum temperature pixel at the current measurement mode 
		 *	measurementData.pMax	- IR coordinate of the maximum temperature pixel at the current measurement mode 
		 * 
		 * On Other measurement modes, or on Non-TH devices, the above fields will have value of 0.

		MeasurementData md = measurementData;
		if (mDeviceSdk.IsThDevice() && measurementData.fMax != 0)
		{
			Log.d("OnFrameGetThermAppTemperatures","Measurement mode data: Min=" + md.fMin + " @ (" + md.pMin.x + "," + md.pMin.y + ") , Max=" + md.fMax + " @ (" + md.pMax.x + "," + md.pMax.y +") , Avg=" + md.fAvg);
		}
		*/
	}

	@Override
	public void onPause()
	{
		super.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (this.isFinishing())
			CloseApp();
	}

	@Override
	public void onResume()
	{


		if (prefs.getString("listMode", "none").equals("2")) // Thermography mode
		{
			// Turn everything on
			textView_minTemp.setVisibility(View.VISIBLE);
			textView_maxTemp.setVisibility(View.VISIBLE);
			imageView_colorsPane.setVisibility(View.VISIBLE);
			barBck.setImageResource(R.drawable.night_vision_nt);
			rLayout_tempView.setVisibility(View.VISIBLE);

			if (prefs.getString("listpalette", "none").equals("1"))
				SetThermalMode_Col();

			else if (prefs.getString("listpalette", "none").equals("2"))
				SetThermalMode_BW();

			else if (prefs.getString("listpalette", "none").equals("3"))
				SetThermalMode_Purp();
		}
		else // NightVision mode
		{
			if (prefs.getString("listNightPalette", "none").equals("1"))
				SetEnhancedMode();
			if (prefs.getString("listNightPalette", "none").equals("2"))
				SetRevEnhancedMode();
			rLayout_tempView.setVisibility(View.GONE);
			textView_minTemp.setVisibility(View.INVISIBLE);
			textView_maxTemp.setVisibility(View.INVISIBLE);
			imageView_colorsPane.setVisibility(View.INVISIBLE);
			barBck.setImageResource(R.drawable.night_vision_t);
		}

		if (prefs.getString("listUnits", "C").equals("C"))
		{
			CtoF[0] = 1;
			CtoF[1] = 0;
			FerCel = "C";
		}
		else if (prefs.getString("listUnits", "C").equals("F"))
		{
			CtoF[0] = 1.8f;
			CtoF[1] = 32;
			FerCel = "F";
		}




		if (null != mDeviceSdk)
		{
			if(!mDeviceSdk.SetIgnoringRatio(0.25f/100f))	// Set the ignoring ratio (Scale Truncation) value to 0.25%
			{
				Log.d("SetIgnoringRatio","Please enter valid value" );
			}
		}
		super.onResume();


	}

	private void CloseApp()
	{
		if (null != mDeviceSdk)
		{
			mDeviceSdk.Close();
		}
		this.finish();
		System.exit(0);
	}

	private boolean InitSdk()
	{
		// Create Developer SDK Instance
		mDeviceSdk = new ThermAppAPI(this);

		// Try to open usb interface
		try
		{
			mDeviceSdk.ConnectToDevice();
		}
		catch (Exception e)
		{
			// Close SDK
			mDeviceSdk = null;
			return false;
		}
		return true;
	}

	private void SetEnhancedMode()
	{
		try
		{
			if (null != mDeviceSdk)
				mDeviceSdk.SetMode(ThermAppAPI.Mode.Enhanced, gray_palette, ThermAppAPI.Coloring_Mode.Normal);
		}
		catch (Exception e) {}

		Bitmap bm = Bitmap.createBitmap(gray_palette, 256, 1,
				Bitmap.Config.ARGB_8888);
		imageView_colorsPane.setImageBitmap(bm);
	}

	private void SetRevEnhancedMode() {
		try {
			if (null != mDeviceSdk)
				mDeviceSdk.SetMode(ThermAppAPI.Mode.Enhanced, mRevGrayPalette, ThermAppAPI.Coloring_Mode.Normal);

		} catch (Exception e) {
		}

		Bitmap bm = Bitmap.createBitmap(mRevGrayPalette, 256, 1, Bitmap.Config.ARGB_8888);
		imageView_colorsPane.setImageBitmap(bm);
	}

	private void SetThermalMode_Col()
	{
		try
		{
			if (null != mDeviceSdk)
				mDeviceSdk.SetMode(ThermAppAPI.Mode.Thermography, therm_palette, ThermAppAPI.Coloring_Mode.Normal);
		}
		catch (Exception e) {}

		Bitmap bm = Bitmap.createBitmap(therm_palette, 256, 1,
				Bitmap.Config.ARGB_8888);
		imageView_colorsPane.setImageBitmap(bm);
	}

	private void SetThermalMode_BW()
	{
		try
		{
			if (null != mDeviceSdk)
				mDeviceSdk.SetMode(ThermAppAPI.Mode.Thermography, gray_palette, ThermAppAPI.Coloring_Mode.Normal);
		}
		catch (Exception e) {}

		Bitmap bm = Bitmap.createBitmap(gray_palette, 256, 1,
				Bitmap.Config.ARGB_8888);
		imageView_colorsPane.setImageBitmap(bm);
	}

	private void SetThermalMode_Purp()
	{
		try
		{
			if (null != mDeviceSdk)
				mDeviceSdk.SetMode(ThermAppAPI.Mode.Thermography, my_palette, ThermAppAPI.Coloring_Mode.Normal);
		}
		catch (Exception e) {}

		Bitmap bm = Bitmap.createBitmap(my_palette, 256, 1,
				Bitmap.Config.ARGB_8888);
		imageView_colorsPane.setImageBitmap(bm);
	}

	private int[] createPalette(int sR, int sG, int sB, int eR, int eG, int eB)
	{
		int[] my_palette = new int[256];
		float pr;
		float Red;
		float Green;
		float Blue;

		for (int i = 0; i < 256; i++)
		{
			pr = (float) i / (float) 256;
			Red = sR * pr + eR * (1 - pr);
			Green = sG * pr + eG * (1 - pr);
			Blue = sB * pr + eB * (1 - pr);

			my_palette[i] = 0xFF000000 | (Math.round(Red) << 16)
					| (Math.round(Green) << 8) | (Math.round(Blue) << 0);
		}
		return my_palette;
	}

	private void addListenerOnButtons()
	{
		iv = (ImageView) findViewById(R.id.imageView_tempView);

		textView_temp = (TextView) findViewById(R.id.textView_temperatureCenterCross);
		rLayout_tempView = (RelativeLayout) findViewById(R.id.RLayout_tempView);

		textView_minTemp = (TextView) findViewById(R.id.textView_minTemp);
		textView_maxTemp = (TextView) findViewById(R.id.textView_maxTemp);
		textView_linkToThermAppWebsite = (TextView) findViewById(R.id.linkToThermAppWebsite);

		imageView_colorsPane = (ImageView) findViewById(R.id.imageView_colorsPane);
		rLayout_initializationScreen = (RelativeLayout) findViewById(R.id.RLayout_initializationScreen);
		rLayout_initializationScreen.setVisibility(View.VISIBLE);

		rLayout_noCameraScreen = (RelativeLayout) findViewById(R.id.RLayout_noCameraScreen);
		barBck = (ImageView) findViewById(R.id.imageView_middleBarBackground);

		CtoF = new float[2];

		textView_linkToThermAppWebsite.setClickable(true);
		textView_linkToThermAppWebsite.setMovementMethod(LinkMovementMethod.getInstance());
		String text = "<a href='http://www.therm-app.com'>www.therm-app.com</a>";
		textView_linkToThermAppWebsite.setText(Html.fromHtml(text));

		button_settings = (ImageButton) findViewById(R.id.btnSettings);
		button_settings.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent in = new Intent(MainActivity.this, SettingsActivity.class);
				in.putExtra("serialnum", Integer.toString(mDeviceSdk.GetSerialNumber()));
				startActivity(in);
			}
		});
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == 1) {
			if (resultCode == RESULT_OK)
			{
				String result = data.getStringExtra("result");

				if (result.equals("OK"))
				{
					try
					{
						mDeviceSdk.StartVideo();
						mDeviceSdk.SetTmaxTmin(mDeviceSdk.ciAUTO_TEMP_INDICATOR, mDeviceSdk.ciAUTO_TEMP_INDICATOR);	// Sets minimum and maximum temperature to be in automatic mode
					}
					catch (Exception e)
					{
						// Report error to use
						AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
						dlgAlert.setMessage("Unable to start video: " + e.getMessage());
						dlgAlert.setTitle("HTBio");
						dlgAlert.setPositiveButton("OK", null);
						dlgAlert.setCancelable(true);
						dlgAlert.create().show();
					}

					rLayout_initializationScreen.setVisibility(View.GONE);
				}
				else if (result.equals("EXIT"))
				{
					CloseApp();
				}
			}
			if (resultCode == RESULT_CANCELED)
			{
				CloseApp();
			}
		}
	}

	/*
	 * The old onCreate function
	 * */
	public void onCreateMethod()
	{



		// Initializes ThermApp SDK
		if (InitSdk() && mDeviceSdk.IsValidSerial())
		{
			try
			{
				// Start Image Processing
				mDeviceSdk.AfterConnect();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				Toast.makeText(getApplicationContext(), "Unable to start image processing!", Toast.LENGTH_LONG).show();
			}

			sendAllPalette();

			if (mDeviceSdk.IsThDevice())
			{
				//mDeviceSdk.calcNotFixedTemp(1,1,1);
				Toast.makeText(getApplicationContext(), "ThermApp-TH device detected!", Toast.LENGTH_LONG).show();

				// Switch to Thermography mode if TH device is connected
				prefs.edit().putString("listMode", "2").commit();		// Set Thermography mode
				prefs.edit().putString("listpalette", "1").commit();	// Set Rainbow palette

				SetThermalMode_Col();	// Change mode

				/*
				 * On ThermApp-TH devices, you can change the measurement to AREA/LINE/HILO.
				 * The following example requests the API to report back (at the OnFrameGetThermAppTemperatures callback) the minimum and maximum temperatures 
				 * of the entire frame, their coordinates and average.
				 * Note: Bad pixels on the measured area can produce bad results. 

				Point point_start = new Point(0,mDeviceSdk.GetHeight()-1);		// start measurement point (top left of the frame considering portrait mode)
				Point point_end = new Point(mDeviceSdk.GetWidth()-1,0);	// end measurement point (bottom right of the frame considering portrait mode)
				try 
				{
					mDeviceSdk.SetMeasurementMode(Measurement_Mode.HILO, point_start, point_end);
				}
				catch (Exception e) 
				{
					Log.e("SetMeasurementMode()","Measurement points are possibly invalid"); 
				}
				 */
			}

			final Intent i = new Intent(this, WelcomeActivity.class);
			i.putExtra("serialnum", Integer.toString(mDeviceSdk.GetSerialNumber()));
			startActivityForResult(i, 1);
		}
		else
		{
			rLayout_noCameraScreen.setVisibility(View.VISIBLE);
		}



		// Define USB detached event receiver
		BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
		{
			public void onReceive(Context context, Intent intent)
			{
				if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction()))
					CloseApp();
			}
		};

		// Listen for new devices
		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		File folder = new File(media_path);
		if (!folder.exists())
			folder.mkdir();
	}

	/*
	 * This method receive the result of the permission request.
	 * if the user granted the permissions that are needed then the onCreateMethod will start the application.
	 * in case that the permissions denied by the user, a message will appear on the screen and the user can choose whether to exit the application or grant the permissions. 
	 * */
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		switch (requestCode) {
			case PHONE_STROGE_PERMISSION:
			{
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
				{
					// Permission Granted
					try
					{
						onCreateMethod();
					}
					catch (Exception e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else
				{// Permission Denied
					new AlertDialog.Builder(MainActivity.this)
							.setMessage("The application need PHONE state permission to recognize the usb and STORAGE permision to process the video.")
							.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE,Manifest.permission.READ_EXTERNAL_STORAGE},PHONE_STROGE_PERMISSION);
								}
							})
							.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									System.exit(0);
								}
							})
							.create()
							.show();
				}
			}
			break;
			default:
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		addListenerOnButtons();

		matrix_imrot_90 = new Matrix();
		matrix_imrot_90.postRotate(90);

		CreatePalettes();
		prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		/*
		 * In android API 23 and above, the permissions have to be requested at run time.
		 * In order to use ThermApp API the application needs 2 permissions to be granted:
		 * 1) android.permission-group.STORAGE (we ask for Manifest.permission.READ_EXTERNAL_STORAGE permission who is subgroup of android.permission-group.STORAGE, when one subgroup get permission all the group receive it)
		 * 2) android.permission-group.PHONE (we ask for Manifest.permission.READ_PHONE_STATE permission who is subgroup of android.permission-group.STORAGE, when one subgroup get permission all the group receive it)
		 * 
		 * In the onCreate function we are checking for permission, if the android API is lower than API 23 then we will run the onCreateMethod that will start the application.
		 * For API 23 or above the permissions will be checked at run time and will start the application if the user granted those permissions.
		 * The result (Allow/Deny) received in onRequestPermissionsResult method.
		 * */
		if (Build.VERSION.SDK_INT >= 23) 	//check android API, if 23 or higher, need to ask permission, else run onCreateMethod.
		{	//check both permission that the ThemApp API needed, if not granted than explain why we need them and ask for them.
			if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE)!= PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
			{
				// Should we show an explanation?, if the permission was asked more then one time.
				if (shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE) || shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE))
				{
					new AlertDialog.Builder(MainActivity.this)
							.setMessage("The application need PHONE state permission to recognize the usb and STORAGE permision to process the video.")
							.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {	// the user decide to continue, show the request.
									requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE,Manifest.permission.READ_EXTERNAL_STORAGE},PHONE_STROGE_PERMISSION);
								}
							})
							.setNegativeButton("Exit", new DialogInterface.OnClickListener() { // the user decide to exit ThermApp.
								@Override
								public void onClick(DialogInterface dialog, int which) {
									System.exit(0);
								}
							})
							.create()
							.show();
				}
				else
				{
					// No explanation needed, we can request the permission.
					requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE,Manifest.permission.READ_EXTERNAL_STORAGE},PHONE_STROGE_PERMISSION);
					return;
				}
			}
			else
			{
				onCreateMethod();
			}
		}
		else
		{
			onCreateMethod();
		}
	}

	@Override
	public void onBackPressed()
	{
		CloseApp();
		super.onBackPressed();
	}

	@Override
	protected void onNewIntent(Intent intent)
	{

		if (rLayout_initializationScreen.getVisibility() == View.VISIBLE)
		{
			onCreate(new Bundle());
		}
		super.onNewIntent(intent);
	}

	private void CreatePalettes()
	{
		gray_palette = new int[256];
		mRevGrayPalette= new int[256];
		int j=255;
		for (int i = 0; i < 256; i++) {
			gray_palette[i] = 0xFF000000 | (i << 0) | (i << 8) | (i << 16);
			mRevGrayPalette[j] = 0xFF000000 | (i << 0) | (i << 8) | (i << 16);
			j--;
		}

		int PALETTE_MAX_IND = 256 - 1;
		therm_palette = new int[256];

		therm_palette[0]= 0xFF000083;
		for (int i=1 ; i<32 ; i++)
		{
			therm_palette[i]= therm_palette[i-1]+4;
		}
		therm_palette[32]= 0xFF0004ff;
		for (int i=33; i<95 ;i++)
		{
			therm_palette[i]= therm_palette[i-1]+0x400;
		}
		therm_palette[95]= 0xFF00ffff;
		therm_palette[96]= 0xFF04fffb;
		for (int i=97; i<159 ;i++)
		{
			therm_palette[i]= therm_palette[i-1]+0x3FFFC;
		}
		therm_palette[159]= 0xFFffff00;
		for (int i=160; i<223 ;i++)
		{
			therm_palette[i]= therm_palette[i-1]-0x400;
		}
		therm_palette[223]=0xFFfb0000 ;

		for (int i=224; i<256 ;i++)
		{
			therm_palette[i]= therm_palette[i-1]-0x40000;
		}

		my_palette = createPalette(253, 250, 0, 86, 0, 154);
	}


	/**
	 * This function turn on the gain limit mode.
	 * The gain limit mode cleans the noise on the image when the differences between maximum and minimum temperature are below 3 Celsius degrees.
	 * @param gainModeActivate	-	true = activate gain limit mode, 
	 * 								false = deactivate gain limit mode.
	 */
	private void setGainLimitMode(boolean gainModeActivate)
	{
		mDeviceSdk.setmGainLimitMode(gainModeActivate);
	}

	/**
	 * This function will set all your own palettes to the API.
	 * when isShowAllPalette = 1 the API create bitmap for each palette simultaneously.
	 * EXAMPLE: in this function we set 3 palettes, after setting isShowAllPalette to 1 we will receive the same image 3 times
	 *  with different colors in the callback (allBitmap parameter). 
	 *
	 *  Works only one time, to get Another set of images we need to set again 1 in isShowAllPalette.
	 */
	private void sendAllPalette()
	{
		int[][] all = new int[4][];
		all[0] = gray_palette;
		all[1] = therm_palette;
		all[2] = my_palette;
		all[3] = mRevGrayPalette;

		mDeviceSdk.setAllPalette(all, 4);
	}
}