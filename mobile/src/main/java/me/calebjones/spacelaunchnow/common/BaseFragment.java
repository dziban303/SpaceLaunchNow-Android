package me.calebjones.spacelaunchnow.common;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

import io.realm.Realm;
import me.calebjones.spacelaunchnow.utils.analytics.Analytics;

public class BaseFragment extends Fragment {
    private Realm realm;
    private String screenName = "Unknown (Name not set)";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        realm = Realm.getDefaultInstance();
        Analytics.getInstance().sendScreenView(screenName, screenName + " started.");
    }

    @Override
    public void onStop() {
        super.onStop();
        realm.removeAllChangeListeners();
        realm.close();
    }

    @Override
    public void onResume() {
        super.onResume();
        Analytics.getInstance().sendScreenView(screenName, screenName + " resumed.");
    }

    @Override
    public void onPause() {
        super.onPause();
        Analytics.getInstance().notifyGoneBackground();
    }

    public Realm getRealm() {
        return realm;
    }

    public void setScreenName(String screenName){
        this.screenName = screenName;
    }
}
