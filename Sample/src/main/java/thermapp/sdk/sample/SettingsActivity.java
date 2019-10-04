/*
 * Setting Activity screen
 */

package thermapp.sdk.sample;

import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.widget.EditText;

import com.google.analytics.tracking.android.EasyTracker;
import thermapp.sdk.sample.R;


public class SettingsActivity extends PreferenceActivity
{

	static ListPreference mlistMode;
	static ListPreference mlistPalette;
	static ListPreference mnightVisionListPalette;

	static ListPreference unist;

	static Context context;
	static String serialnum;
	static String version_n;

	static Preference version;
	static Preference serial;

	static EditTextPreference saveRate;
	static String default_rate;

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
	protected void onCreate(final Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new MyPreferenceFragment())
				.commit();

		PackageInfo pInfo = null;
		try 
		{
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		}
		catch (NameNotFoundException e) 
		{
			e.printStackTrace();
		}
		version_n = pInfo.versionName;

		context = getApplicationContext();
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			serialnum = extras.getString("serialnum");
		}
	}



	public static class MyPreferenceFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener 
	{

		@Override
		public void onResume() 
		{
			super.onResume();

			getPreferenceScreen().getSharedPreferences()
					.registerOnSharedPreferenceChangeListener(this);
		}

		@Override
		public void onCreate(final Bundle savedInstanceState) 
		{
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.prefs);

			for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) 
			{
				updatePrefsSummary(getPreferenceManager()
						.getSharedPreferences(), getPreferenceScreen()
						.getPreference(i));
			}

			mlistMode = (ListPreference) findPreference("listMode");
			mlistPalette = (ListPreference) findPreference("listpalette");
			mnightVisionListPalette = (ListPreference) findPreference("listNightPalette");
			unist = (ListPreference) findPreference("listUnits");
			unist.setOnPreferenceChangeListener(units_listener);
			mlistMode.setOnPreferenceChangeListener(listener);
			version = (Preference) findPreference("version");
			serial = (Preference) findPreference("serial");

			saveRate = (EditTextPreference) findPreference("FrameRate");
			saveRate.setOnPreferenceChangeListener(saveRate_listener);

			//Long.valueOf( prefs.getString("saveRate", "10"))




			version.setEnabled(false);
			serial.setEnabled(false);

			version.setSummary(version_n);
			serial.setSummary(serialnum);

			if (mlistMode.getValue().toString().equals("1"))  //nifght vision
			{
				mlistPalette.setEnabled(false);
				unist.setEnabled(false);
				mnightVisionListPalette.setEnabled(true);
			}
			else if (mlistMode.getValue().equals("2")) //thermography vision
			{
				mlistPalette.setEnabled(true);
				unist.setEnabled(true);
				mnightVisionListPalette.setEnabled(false);
			}
		}

		OnPreferenceClickListener download_bin_listener = new OnPreferenceClickListener() 
		{
			@Override
			public boolean onPreferenceClick(Preference preference) 
			{
				Intent i = new Intent(context, WelcomeActivity.class);
				i.putExtra("serialnum", serialnum);
				startActivity(i);
				return false;
			}
		};

		OnPreferenceChangeListener listener = new OnPreferenceChangeListener()
		{
			@Override
			public boolean onPreferenceChange(Preference preference,Object newValue) 
			{
				mlistMode.setValue((String) newValue);

				if (newValue.toString().equals("1")) //nifght vision
				{
					mlistPalette.setEnabled(false);
					unist.setEnabled(false);
					mnightVisionListPalette.setEnabled(true);
				}
				else if (newValue.toString().equals("2")) //thermography vision
				{
					mlistPalette.setEnabled(true);
					unist.setEnabled(true);
					mnightVisionListPalette.setEnabled(false);
				}
				return false;
			}
		};


		OnPreferenceChangeListener saveRate_listener = new OnPreferenceChangeListener()
		{
			@Override
			public boolean onPreferenceChange(Preference preference,Object newValue)
			{

				saveRate.setText(newValue.toString());
				/*
				getPreferenceScreen().getSharedPreferences().edit()
						.putString("saveRate", newValue.toString()).commit();
						*/
				return false;
			}
		};

		OnPreferenceChangeListener units_listener = new OnPreferenceChangeListener() 
		{
			@Override
			public boolean onPreferenceChange(Preference preference,Object newValue) 
			{
				unist.setValue((String) newValue);
				if (newValue.toString().equals("C")) 
				{
					getPreferenceScreen().getSharedPreferences().edit()
							.putString("reflected_temp", "20").commit();
				}
				else if (newValue.toString().equals("F")) 
				{
					getPreferenceScreen().getSharedPreferences().edit()
							.putString("reflected_temp", "68").commit();
				}
				return false;
			}
		};

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) 
		{
			updatePrefsSummary(sharedPreferences, findPreference(key));
		}

	}

	protected static void updatePrefsSummary(SharedPreferences sharedPreferences, Preference pref) 
	{
		if (pref == null)
			return;

		if (pref instanceof ListPreference) 
		{
			ListPreference listPref = (ListPreference) pref;
			listPref.setSummary(listPref.getEntry());

		}
		else if (pref instanceof EditTextPreference) 
		{
			EditTextPreference editTextPref = (EditTextPreference) pref;
			editTextPref.setSummary(editTextPref.getText());

		}
		else if (pref instanceof Preference) 
		{

		}
		else if (pref instanceof MultiSelectListPreference) 
		{
			MultiSelectListPreference mlistPref = (MultiSelectListPreference) pref;
			String summaryMListPref = "";
			String and = "";

			Set<String> values = mlistPref.getValues();
			for (String value : values) 
			{
				int index = mlistPref.findIndexOfValue(value);
				CharSequence mEntry = index >= 0
						&& mlistPref.getEntries() != null ? mlistPref
						.getEntries()[index] : null;
				if (mEntry != null) 
				{
					summaryMListPref = summaryMListPref + and + mEntry;
					and = ";";
				}
			}
			mlistPref.setSummary(summaryMListPref);
		}
	}
}
