/* 
 * This program is free software. It comes without any warranty, to
 * the extent permitted by applicable law. You can redistribute it
 * and/or modify it under the terms of the Do What The Fuck You Want
 * To Public License, Version 2, as published by Sam Hocevar. See
 * http://sam.zoy.org/wtfpl/COPYING for more details.
 */

package com.android.launcherex;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity implements OnPreferenceChangeListener {
	private static Launcher mLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        findPreference(getResources().getString(R.string.preference_1_key)).setOnPreferenceChangeListener(this);
        findPreference(getResources().getString(R.string.preference_2_key)).setOnPreferenceChangeListener(this);
        findPreference(getResources().getString(R.string.preference_3_key)).setOnPreferenceChangeListener(this);
        findPreference(getResources().getString(R.string.preference_4_key)).setOnPreferenceChangeListener(this);
        findPreference(getResources().getString(R.string.preference_5_key)).setOnPreferenceChangeListener(this);
        
        Preference preferencesStyles = (Preference) findPreference("preferencesStyles");
        preferencesStyles.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        	public boolean onPreferenceClick(Preference preference) {
        		Intent i = new Intent(getBaseContext(), Styles.class);
            	startActivity(i);
            	overridePendingTransition(R.anim.drawer_fade_in, R.anim.drawer_fade_out);
                return true;
            }
        });
    }
    
    @Override
	public boolean onPreferenceChange(final Preference preference, final Object newValue) {
		if (preference.getKey().equals(getResources().getString(R.string.preference_3_key)) || preference.getKey().equals(getResources().getString(R.string.preference_5_key)) && mLauncher != null)
			mLauncher.finish();
		Styles.sharedPref.edit().putBoolean(preference.getKey(), ((Boolean) newValue).booleanValue()).commit();
		return true;
	}
    
    public static void setLauncher(Launcher launcher) {
    	mLauncher = launcher;
    }
	
	public void onBackPressed() {
		finish();
	}
}