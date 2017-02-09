package me.calebjones.spacelaunchnow.subscriptions;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;
import me.calebjones.spacelaunchnow.R;
import me.calebjones.spacelaunchnow.common.BaseFragment;
import me.calebjones.spacelaunchnow.content.models.Constants;
import me.calebjones.spacelaunchnow.data.models.realm.AgencySwitch;
import me.calebjones.spacelaunchnow.data.models.realm.LocationSwitch;
import me.calebjones.spacelaunchnow.utils.SnackbarHandler;

public class SubscriptionFragment extends BaseFragment implements SubscriptionContract.View {

    @BindView(R.id.location_view)
    LinearLayout locationView;
    @BindView(R.id.agency_view)
    LinearLayout agencyView;
    @BindView(R.id.arianespace_switch)
    AppCompatCheckBox arianespaceSwitch;
    @BindView(R.id.casc_switch)
    AppCompatCheckBox cascSwitch;
    @BindView(R.id.isro_switch)
    AppCompatCheckBox isroSwitch;
    @BindView(R.id.cape_switch)
    AppCompatCheckBox capeSwitch;
    @BindView(R.id.nasa_switch)
    AppCompatCheckBox nasaSwitch;
    @BindView(R.id.spacex_switch)
    AppCompatCheckBox spacexSwitch;
    @BindView(R.id.roscosmos_switch)
    AppCompatCheckBox roscosmosSwitch;
    @BindView(R.id.ula_switch)
    AppCompatCheckBox ulaSwitch;
    @BindView(R.id.KSC_switch)
    AppCompatCheckBox kscSwitch;
    @BindView(R.id.van_switch)
    AppCompatCheckBox vanSwitch;
    @BindView(R.id.orbital_switch)
    AppCompatCheckBox orbitalSwitch;
    @BindView(R.id.subscription_coordinator_layout)
    CoordinatorLayout coordinatorLayout;

    @BindViews({R.id.arianespace_switch, R.id.casc_switch, R.id.isro_switch,
            R.id.cape_switch, R.id.nasa_switch, R.id.spacex_switch, R.id.roscosmos_switch,
            R.id.ula_switch, R.id.KSC_switch, R.id.van_switch, R.id.orbital_switch})
    List<AppCompatCheckBox> nameViews;

    private SubscriptionContract.Presenter subscriptionPresenter;

    private List<AppCompatCheckBox> customViews = new ArrayList<>();

    public SubscriptionFragment() {
        // Requires empty public constructor
    }

    public static SubscriptionFragment newInstance() {
        return new SubscriptionFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_subscription, container, false);
        ButterKnife.bind(this, root);
        subscriptionPresenter.initializeSwitches();
        subscriptionPresenter.initializeDynamicSwitches(getContext());
        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.subscriptions, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_check_all:
                subscriptionPresenter.checkAllClicked(getRealm(), true);
                break;
            case R.id.action_remove_all:
                subscriptionPresenter.checkAllClicked(getRealm(), false);
                break;
        }
        return true;

    }

    @Override
    public void setPresenter(SubscriptionContract.Presenter presenter) {
        subscriptionPresenter = presenter;
    }

    @Override
    public void showSnackbarMessage(String message) {
        SnackbarHandler.showInfoSnackbar(getContext(), coordinatorLayout, message);
    }

    @Override
    public void updateAllSwitches(final boolean state) {
        for (final AppCompatCheckBox checkbox : nameViews) {
            updateView(checkbox, state);
        }
        for (final AppCompatCheckBox checkBox : customViews) {
            updateView(checkBox, state);
        }
    }

    @Override
    public void updateSwitches() {
        Realm realm = Realm.getDefaultInstance();

        arianespaceSwitch.setChecked(realm.where(AgencySwitch.class)
                                             .equalTo("id", Constants.AGENCY_ARIANESPACE)
                                             .findFirst()
                                             .isSubscribed());

        if (realm.where(LocationSwitch.class).equalTo("id", Constants.LOCATION_CASC[0]).findFirst().isSubscribed() &&
                realm.where(LocationSwitch.class).equalTo("id", Constants.LOCATION_CASC[1]).findFirst().isSubscribed()) {
            cascSwitch.setChecked(true);
        } else {
            cascSwitch.setChecked(false);
        }

        isroSwitch.setChecked(realm.where(AgencySwitch.class)
                                      .equalTo("id", Constants.AGENCY_ISRO)
                                      .findFirst()
                                      .isSubscribed());

        capeSwitch.setChecked(realm.where(AgencySwitch.class)
                                      .equalTo("id", Constants.LOCATION_CAPE)
                                      .findFirst()
                                      .isSubscribed());

        nasaSwitch.setChecked(realm.where(AgencySwitch.class)
                                      .equalTo("id", Constants.AGENCY_NASA)
                                      .findFirst()
                                      .isSubscribed());

        spacexSwitch.setChecked(realm.where(AgencySwitch.class)
                                        .equalTo("id", Constants.AGENCY_SPACEX)
                                        .findFirst()
                                        .isSubscribed());

        if (realm.where(AgencySwitch.class).equalTo("id", Constants.AGENCY_ROSCOSMOS[0]).findFirst().isSubscribed() &&
                realm.where(AgencySwitch.class).equalTo("id", Constants.AGENCY_ROSCOSMOS[1]).findFirst().isSubscribed() &&
                realm.where(AgencySwitch.class).equalTo("id", Constants.AGENCY_ROSCOSMOS[2]).findFirst().isSubscribed() &&
                realm.where(AgencySwitch.class).equalTo("id", Constants.AGENCY_ROSCOSMOS[3]).findFirst().isSubscribed() &&
                realm.where(LocationSwitch.class).equalTo("id", Constants.LOCATION_ROSCOSMOS[0]).findFirst().isSubscribed() &&
                realm.where(LocationSwitch.class).equalTo("id", Constants.LOCATION_ROSCOSMOS[1]).findFirst().isSubscribed() &&
                realm.where(LocationSwitch.class).equalTo("id", Constants.LOCATION_ROSCOSMOS[2]).findFirst().isSubscribed() &&
                realm.where(LocationSwitch.class).equalTo("id", Constants.LOCATION_ROSCOSMOS[3]).findFirst().isSubscribed() &&
                realm.where(LocationSwitch.class).equalTo("id", Constants.LOCATION_ROSCOSMOS[4]).findFirst().isSubscribed() &&
                realm.where(LocationSwitch.class).equalTo("id", Constants.LOCATION_ROSCOSMOS[5]).findFirst().isSubscribed()) {
            roscosmosSwitch.setChecked(true);
        } else {
            roscosmosSwitch.setChecked(false);
        }

        ulaSwitch.setChecked(realm.where(AgencySwitch.class)
                                     .equalTo("id", Constants.AGENCY_ULA)
                                     .findFirst()
                                     .isSubscribed());

        kscSwitch.setChecked(realm.where(LocationSwitch.class)
                                     .equalTo("id", Constants.LOCATION_KSC)
                                     .findFirst()
                                     .isSubscribed());

        vanSwitch.setChecked(realm.where(LocationSwitch.class)
                                     .equalTo("id", Constants.LOCATION_VAN)
                                     .findFirst()
                                     .isSubscribed());

        orbitalSwitch.setChecked(realm.where(AgencySwitch.class)
                                         .equalTo("id", Constants.AGENCY_ORBITAL_ATK)
                                         .findFirst()
                                         .isSubscribed());
        realm.close();
    }

    @Override
    public void addViewToCustomList(AppCompatCheckBox appCompatCheckBox) {
        customViews.add(appCompatCheckBox);
    }

    @Override
    public void addViewToLocationView(AppCompatCheckBox appCompatCheckBox) {
        locationView.addView(appCompatCheckBox);
    }

    @Override
    public void addViewToAgencyView(AppCompatCheckBox appCompatCheckBox) {
        agencyView.addView(appCompatCheckBox);
    }

    @Override
    public void updateCheckboxByID(int id, boolean state) {
        AppCompatCheckBox appCompatCheckBox = (AppCompatCheckBox) getView().findViewById(id);
        appCompatCheckBox.setChecked(state);
    }

    @Override
    public void updateView(AppCompatCheckBox appCompatCheckBox, boolean state) {
        appCompatCheckBox.setChecked(state);
    }

    @Override
    public AppCompatCheckBox createCheckbox(AgencySwitch agencySwitch) {
        AppCompatCheckBox checkbox = new AppCompatCheckBox(getContext());
        checkbox.setChecked(agencySwitch.isSubscribed());
        checkbox.setText(agencySwitch.getName());
        checkbox.setId(agencySwitch.getName().substring(0, Math.min(agencySwitch.getName().length(), 5)).hashCode());
        checkbox.setTextColor(ContextCompat.getColor(getContext(), R.color.material_color_white));
        checkbox.setLayoutParams(getLayoutParams());
        return checkbox;
    }

    @Override
    public AppCompatCheckBox createCheckbox(LocationSwitch locationSwitch) {
        AppCompatCheckBox checkbox = new AppCompatCheckBox(getContext());
        checkbox.setChecked(locationSwitch.isSubscribed());
        checkbox.setText(locationSwitch.getName());
        checkbox.setId(locationSwitch.getName().substring(0, Math.min(locationSwitch.getName().length(), 5)).hashCode());
        checkbox.setTextColor(ContextCompat.getColor(getContext(), R.color.material_color_white));
        checkbox.setLayoutParams(getLayoutParams());
        return checkbox;
    }

    @OnClick({R.id.arianespace_switch, R.id.casc_switch, R.id.isro_switch,
            R.id.cape_switch, R.id.orbital_switch, R.id.nasa_switch,
            R.id.spacex_switch, R.id.roscosmos_switch, R.id.ula_switch,
            R.id.KSC_switch, R.id.van_switch})
    public void checkboxSelected(final AppCompatCheckBox view) {
        subscriptionPresenter.checkboxSelected(view);
    }

    public ViewGroup.LayoutParams getLayoutParams() {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        layoutParams.setMargins(4, 4, 4, 4);
        return layoutParams;
    }
}
