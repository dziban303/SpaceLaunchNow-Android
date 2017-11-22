package me.calebjones.spacelaunchnow.ui.settings.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.support.v7.app.AppCompatDelegate;
import android.widget.Toast;

import com.jaredrummler.android.colorpicker.ColorPreference;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import me.calebjones.spacelaunchnow.R;
import me.calebjones.spacelaunchnow.content.database.SwitchPreferences;
import me.calebjones.spacelaunchnow.ui.main.MainActivity;
import me.calebjones.spacelaunchnow.ui.supporter.SupporterHelper;
import me.calebjones.spacelaunchnow.utils.analytics.Analytics;
import me.calebjones.spacelaunchnow.widget.WidgetBroadcastReceiver;
import timber.log.Timber;

public class AppearanceFragment extends BaseSettingFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SwitchPreferences switchPreferences;
    private Context context;
    private Preference widgetPresets;
    private ColorPreference widgetBackgroundColor;
    private ColorPreference widgetTextColor;
    private ColorPreference widgetSecondaryTextColor;
    private ColorPreference widgetIconColor;
    private boolean isCustomColor = false;
    private int[] textPrimaryArray;
    private int[] textSecondaryArray;
    private int[] backgroundArray;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.appearance_preferences);
        switchPreferences = SwitchPreferences.getInstance(getActivity());
        context = getActivity();
        textPrimaryArray = getResources().getIntArray(R.array.widget_presets_values_text_primary);
        textSecondaryArray = getResources().getIntArray(R.array.widget_presets_values_text_secondary);
        backgroundArray = getResources().getIntArray(R.array.widget_presets_values_background);
        setupPreferences();
        setName("Appearance Fragment");
    }

    @Override
    public void onResume() {
        Timber.v("onResume - setting OnSharedPreferenceChangeListener");
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        Timber.v("onPause - removing OnSharedPreferenceChangeListener");
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Timber.i("Appearance preference %s changed.", key);
        if (key.equals("theme")) {
            SharedPreferences.Editor themeEditor = getActivity().getSharedPreferences("theme_changed", 0).edit();
            themeEditor.putBoolean("recreate", true);
            themeEditor.apply();

            if (switchPreferences.getNightMode()) {
                Analytics.from(this).sendPreferenceEvent(key, sharedPreferences.getBoolean(key, false));
                if (switchPreferences.getDayNightAutoMode()) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
                    checkLocationPermission();
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    Toast.makeText(context, "Night mode might need to restart app to take effect.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Day mode might need to restart app to take effect.", Toast.LENGTH_SHORT).show();
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.setAction("me.calebjones.spacelaunchnow.NIGHTMODE");
            startActivity(intent);
        }
        if (key.equals("theme_auto")) {
            Analytics.from(this).sendPreferenceEvent(key, sharedPreferences.getBoolean(key, false));
            SharedPreferences.Editor themeEditor = getActivity().getSharedPreferences("theme_changed", 0).edit();
            themeEditor.putBoolean("recreate", true);
            themeEditor.apply();

            if (switchPreferences.getNightMode()) {
                if (switchPreferences.getDayNightAutoMode()) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
                    checkLocationPermission();
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    Toast.makeText(context, "Auto DayNight disabled, might need to restart app to take effect.", Toast.LENGTH_SHORT).show();
                }
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.setAction("me.calebjones.spacelaunchnow.NIGHTMODE");
            startActivity(intent);
        } else {
            Analytics.from(this).sendPreferenceEvent(key);
        }

        if (key.equals("widget_background_color") || key.equals("widget_text_color") || key.equals("widget_secondary_text_color") || key.equals("widget_icon_color")) {
            Intent nextIntent = new Intent(context, WidgetBroadcastReceiver.class);
            nextIntent.putExtra("updateUIOnly", true);
            context.sendBroadcast(nextIntent);
        }

        if  (key.equals("widget_theme_round_corner")){
            Intent nextIntent = new Intent(context, WidgetBroadcastReceiver.class);
            nextIntent.putExtra("updateUIOnly", true);
            context.sendBroadcast(nextIntent);
        }

        if (key.equals("widget_presets")) {

            checkWidgetPreset(Integer.parseInt(sharedPreferences.getString("widget_presets", "2")));
            Intent nextIntent = new Intent(context, WidgetBroadcastReceiver.class);
            nextIntent.putExtra("updateUIOnly", true);
            context.sendBroadcast(nextIntent);
        }
    }

    private void checkWidgetPreset(Integer arrayPosition) {
        int backgroundColor = backgroundArray[arrayPosition];
        int primrayTextColor = textPrimaryArray[arrayPosition];
        int secondaryTextColor = textSecondaryArray[arrayPosition];
        Timber.v("Preset # %d", arrayPosition);
        if (arrayPosition != 8) {
            widgetBackgroundColor.saveValue(backgroundColor);
            widgetTextColor.saveValue(primrayTextColor);
            widgetSecondaryTextColor.saveValue(secondaryTextColor);
            widgetIconColor.saveValue(primrayTextColor);
            isCustomColor = false;
            Timber.v("Applied widget colors");
        } else {
            isCustomColor = true;
        }
    }

    private void setupPreferences() {
        widgetPresets = findPreference("widget_presets");
        widgetBackgroundColor = (ColorPreference) findPreference("widget_background_color");
        widgetTextColor = (ColorPreference) findPreference("widget_text_color");
        widgetSecondaryTextColor = (ColorPreference) findPreference("widget_secondary_text_color");
        widgetIconColor = (ColorPreference) findPreference("widget_icon_color");
        if (!SupporterHelper.isSupporter()) {
            Preference weather = findPreference("weather");
            weather.setEnabled(false);
            weather.setSelectable(false);

            PreferenceCategory prefCatWeather = (PreferenceCategory) findPreference("weather_category");
            prefCatWeather.setTitle(prefCatWeather.getTitle() + " (Supporter Feature)");
            Preference measurement = findPreference("weather_US_SI");
            measurement.setEnabled(false);
            measurement.setSelectable(false);

            PreferenceCategory prefCatWidget = (PreferenceCategory) findPreference("widget_category");
            prefCatWidget.setTitle(prefCatWidget.getTitle() + " (Supporter Feature)");


            widgetPresets.setEnabled(false);
            widgetPresets.setSelectable(false);

            widgetBackgroundColor.setEnabled(false);
            widgetBackgroundColor.setSelectable(false);

            widgetTextColor.setEnabled(false);
            widgetTextColor.setSelectable(false);

            widgetSecondaryTextColor.setEnabled(false);
            widgetSecondaryTextColor.setSelectable(false);
        }
        Preference localTime = findPreference("local_time");
        localTime.setOnPreferenceChangeListener(createLocalTimeListener());
    }

    private Preference.OnPreferenceChangeListener createLocalTimeListener() {
        return new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                Timber.v("Clicked!");
                return true;
            }

        };
    }

    public void checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Dexter.withActivity(getActivity()).withPermission(Manifest.permission.ACCESS_COARSE_LOCATION).withListener(new PermissionListener() {
                @Override
                public void onPermissionGranted(PermissionGrantedResponse response) {

                }

                @Override
                public void onPermissionDenied(PermissionDeniedResponse response) {
                    if (response.isPermanentlyDenied()) {
                        Toast.makeText(context, "Location denied, please go to Android Settings -> Apps to enable.", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                    token.continuePermissionRequest();
                }
            }).check();
        }
    }
}
