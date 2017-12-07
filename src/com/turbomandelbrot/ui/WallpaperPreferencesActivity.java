package com.turbomandelbrot.ui;

import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.*;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.turbomandelbrot.LiveWallpaper;
import com.turbomandelbrot.R;

public class WallpaperPreferencesActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new WallpaperPreferences()).commit();
    }

    public static class WallpaperPreferences extends PreferenceFragment {
        private static SharedPreferences sharedPref;
        private static SharedPreferences.OnSharedPreferenceChangeListener sharedPrefListener;

        private Preference setWallpaper;
        private Preference explore;
        private Preference benchmark;

        @Override
        public void onCreate(Bundle icicle) {
            super.onCreate(icicle);
            addPreferencesFromResource(R.xml.livewallpaper_settings);

            sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            PreferenceManager.setDefaultValues(getActivity(), R.xml.livewallpaper_settings, true);
            sharedPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    Preference pref = findPreference(key);

                    if (pref instanceof ListPreference) {
                        ListPreference listPref = (ListPreference) pref;
                        String title = pref.getTitle().toString();
                        String value = listPref.getEntry().toString();
                        int substr_index = title.lastIndexOf('-') - 1;
                        if(substr_index < 0)
                            substr_index = title.length();
                        pref.setTitle(title.substring(0, substr_index) + " - " + value);

                    }
                }
            };
            sharedPref.registerOnSharedPreferenceChangeListener(sharedPrefListener);

            //Применить изменение названий настроек при включении
            for(String key : sharedPref.getAll().keySet()){
                sharedPrefListener.onSharedPreferenceChanged(sharedPref, key);
            }

            initPreferences();
        }

        private void initPreferences(){
            setWallpaper = getPreferenceManager().findPreference("Set wallpaper");
            setWallpaper.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref){
                    Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                    intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            new ComponentName(getActivity(), LiveWallpaper.class));
                    startActivity(intent);
                    return true;
                }
            });

            explore = getPreferenceManager().findPreference("Explore fractal");
            explore.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference pref){
                    Intent intent = new Intent(getActivity(), ExploreActivity.class);
                    startActivity(intent);
                    return true;
                }
            });
        }

        public void showError(){
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.did_not_pass_requirements_title);
            builder.setMessage(R.string.did_not_pass_requirements_message);
            builder.setCancelable(true);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            builder.show();
        }

        @Override
        public void onResume() {
            super.onResume();
            sharedPref.registerOnSharedPreferenceChangeListener(sharedPrefListener);
        }

        @Override
        public void onPause() {
            super.onPause();
            sharedPref.unregisterOnSharedPreferenceChangeListener(sharedPrefListener);
        }

        @Override
        public void onDestroy() {
            sharedPref.unregisterOnSharedPreferenceChangeListener(sharedPrefListener);
            super.onDestroy();
        }

        @Override
        public void onSaveInstanceState(Bundle outState){
            super.onSaveInstanceState(outState);
        }

        public static SharedPreferences getSharedPreferences(){
            return sharedPref;
        }
    }
}
