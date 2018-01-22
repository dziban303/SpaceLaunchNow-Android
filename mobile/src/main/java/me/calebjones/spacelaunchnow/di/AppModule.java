package me.calebjones.spacelaunchnow.di;

import android.app.Application;
import android.content.Context;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class AppModule {

    private Context context;

    public AppModule(Context context) {
        this.context = context;
    }

    @Binds
    abstract Context provideContext(Application application);

}
