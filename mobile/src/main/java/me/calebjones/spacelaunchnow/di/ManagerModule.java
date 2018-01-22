package me.calebjones.spacelaunchnow.di;


import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import me.calebjones.spacelaunchnow.content.wear.WearWatchfaceManager;

@Module
public class ManagerModule {

    // Dagger will only look for methods annotated with @Provides
    @Provides
    @Singleton
    // Application reference must come from AppModule.class
    SharedPreferences providesSharedPreferences(Application application) {
        return PreferenceManager.getDefaultSharedPreferences(application);
    }

    @Provides
    @Singleton
    public WearWatchfaceManager provideWearWatchfaceManager(Context context) {
        return new WearWatchfaceManager(context);
    }

}
