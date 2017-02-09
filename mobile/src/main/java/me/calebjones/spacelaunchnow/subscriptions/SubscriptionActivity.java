package me.calebjones.spacelaunchnow.subscriptions;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import me.calebjones.spacelaunchnow.R;
import me.calebjones.spacelaunchnow.utils.ActivityUtils;

public class SubscriptionActivity extends AppCompatActivity implements SubscriptionContract.NavigatorProvider {

    private SubscriptionPresenter subscriptionPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);

        SubscriptionFragment subscriptionFragment =
                (SubscriptionFragment) getSupportFragmentManager().findFragmentById(R.id.contentFrame);
        if (subscriptionFragment == null) {
            // Create the fragment
            subscriptionFragment = SubscriptionFragment.newInstance();
            ActivityUtils.addFragmentToActivity(
                    getSupportFragmentManager(), subscriptionFragment, R.id.contentFrame);
        }
        // Set up the toolbar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setTitle("Subscriptions");
        ab.setDisplayHomeAsUpEnabled(true);

        // Create the presenter
        subscriptionPresenter = new SubscriptionPresenter(subscriptionFragment);
        subscriptionPresenter.setNavigator(getNavigator(subscriptionPresenter));

        // Load previously saved state, if available.
        if (savedInstanceState != null) {

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                subscriptionPresenter.onHomeClicked();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @NonNull
    @Override
    public SubscriptionContract.Navigator getNavigator(SubscriptionContract.Presenter presenter) {
        return new SubscriptionNavigator(this);
    }
}
