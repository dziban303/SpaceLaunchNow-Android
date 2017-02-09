package me.calebjones.spacelaunchnow.subscriptions;

import android.support.v7.app.AppCompatActivity;

public class SubscriptionNavigator implements SubscriptionContract.Navigator {

    private AppCompatActivity subscriptionActivity;

    public SubscriptionNavigator(AppCompatActivity activity){
        subscriptionActivity = activity;
    }

    @Override
    public void goHome() {
        subscriptionActivity.onBackPressed();
    }
}
