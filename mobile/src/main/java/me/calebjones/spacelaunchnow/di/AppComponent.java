package me.calebjones.spacelaunchnow.di;

import android.app.Application;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjector;
import dagger.android.DaggerApplication;
import dagger.android.support.AndroidSupportInjectionModule;
import me.calebjones.spacelaunchnow.LaunchApplication;


@Singleton
@Component(modules = {
        AndroidSupportInjectionModule.class,
        AppModule.class})
public interface AppComponent extends AndroidInjector<DaggerApplication>{

    void inject(LaunchApplication app);

    @Override
    void inject(DaggerApplication instance);

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder application(Application application);
        AppComponent build();
    }
}
