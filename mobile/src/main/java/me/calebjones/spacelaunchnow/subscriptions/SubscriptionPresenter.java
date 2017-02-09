package me.calebjones.spacelaunchnow.subscriptions;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.View;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import me.calebjones.spacelaunchnow.R;
import me.calebjones.spacelaunchnow.content.models.Constants;
import me.calebjones.spacelaunchnow.data.models.realm.Agency;
import me.calebjones.spacelaunchnow.data.models.realm.AgencySwitch;
import me.calebjones.spacelaunchnow.data.models.realm.Location;
import me.calebjones.spacelaunchnow.data.models.realm.LocationSwitch;
import timber.log.Timber;

public class SubscriptionPresenter implements SubscriptionContract.Presenter {

    private final SubscriptionContract.View subscriptionView;
    private SubscriptionContract.Navigator navigator;

    public SubscriptionPresenter(SubscriptionContract.View view) {
        subscriptionView = view;
        subscriptionView.setPresenter(this);
    }

    @Override
    public void onHomeClicked() {
        navigator.goHome();
    }

    @Override
    public void setNavigator(@NonNull SubscriptionContract.Navigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public void checkAllClicked(Realm realm, final boolean value) {
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                RealmResults<LocationSwitch> locations = realm.where(LocationSwitch.class).findAll();
                for (LocationSwitch locationSwitch : locations) {
                    locationSwitch.setSubscribed(value);

                    Location location = realm.where(Location.class).equalTo("id", locationSwitch.getId()).findFirst();
                    if (location != null) {
                        location.setSubscribed(locationSwitch.isSubscribed());
                    }
                }
                RealmResults<AgencySwitch> agencies = realm.where(AgencySwitch.class).findAll();
                for (AgencySwitch agencySwitch : agencies) {
                    agencySwitch.setSubscribed(value);

                    Agency agency = realm.where(Agency.class).equalTo("id", agencySwitch.getId()).findFirst();
                    if (agency != null) {
                        agency.setSubscribed(agencySwitch.isSubscribed());
                    }
                }
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                subscriptionView.updateAllSwitches(value);
            }
        });
    }

    @Override
    public void initializeSwitches() {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                RealmResults<LocationSwitch> locationSwitches = realm.where(LocationSwitch.class).findAll();
                RealmResults<AgencySwitch> agencySwitches = realm.where(AgencySwitch.class).findAll();

                for (LocationSwitch locationSwitch : locationSwitches) {
                    Location location = realm.where(Location.class).equalTo("id", locationSwitch.getId()).findFirst();
                    if (location != null) {
                        location.setSubscribed(locationSwitch.isSubscribed());
                    }
                }

                for (AgencySwitch agencySwitch : agencySwitches) {
                    Agency agency = realm.where(Agency.class).equalTo("id", agencySwitch.getId()).findFirst();
                    if (agency != null) {
                        agency.setSubscribed(agencySwitch.isSubscribed());
                    }
                }
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                subscriptionView.updateSwitches();
            }
        });
        realm.close();
    }

    @Override
    public void initializeDynamicSwitches(Context context) {

        Realm realm = Realm.getDefaultInstance();
        Timber.v("initDynamicView");
        final RealmResults<LocationSwitch> locations = realm.where(LocationSwitch.class).findAllSortedAsync("id");
        locations.addChangeListener(new RealmChangeListener<RealmResults<LocationSwitch>>() {
            @Override
            public void onChange(RealmResults<LocationSwitch> element) {
                addDynamicLocationSwitches(element);
                locations.removeChangeListeners();
            }
        });
        RealmResults<AgencySwitch> agencies = realm.where(AgencySwitch.class).findAllSortedAsync("name");
        agencies.addChangeListener(new RealmChangeListener<RealmResults<AgencySwitch>>() {
            @Override
            public void onChange(RealmResults<AgencySwitch> element) {
                addDynamicAgencySwitches(element);
                locations.removeChangeListeners();
            }
        });
        Timber.v("initDynamicView - retrieved");
        realm.close();
    }

    private void addDynamicAgencySwitches(RealmResults<AgencySwitch> agencies) {
        for (AgencySwitch agencySwitch : agencies) {
            AppCompatCheckBox checkbox = subscriptionView.createCheckbox(agencySwitch);
            final int agencySwitchId = agencySwitch.getId();
            checkbox.setOnClickListener(new AppCompatCheckBox.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Realm realm = Realm.getDefaultInstance();
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            AgencySwitch mAgencySwitch = realm.where(AgencySwitch.class).equalTo("id", agencySwitchId).findFirst();
                            mAgencySwitch.setSubscribed(!mAgencySwitch.isSubscribed());
                            initializeSwitches();
                        }
                    });
                    subscriptionView.showSnackbarMessage(String.valueOf(realm.where(AgencySwitch.class).equalTo("id", agencySwitchId).findFirst().isSubscribed()));
                }
            });
            subscriptionView.addViewToCustomList(checkbox);
            subscriptionView.addViewToAgencyView(checkbox);
        }

    }

    private void addDynamicLocationSwitches(RealmResults<LocationSwitch> locations) {
        for (LocationSwitch locationSwitch : locations) {
            if (locationSwitch.getPads() != null && locationSwitch.getPads().size() > 0) {
                final int locationSwitchId = locationSwitch.getId();

                AppCompatCheckBox checkbox = subscriptionView.createCheckbox(locationSwitch);
                checkbox.setOnClickListener(new AppCompatCheckBox.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Realm realm = Realm.getDefaultInstance();
                        syncSwitchStateAsync(realm);
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                LocationSwitch mLocationSwitch = realm.where(LocationSwitch.class).equalTo("id", locationSwitchId).findFirst();
                                mLocationSwitch.setSubscribed(!mLocationSwitch.isSubscribed());
                                initializeSwitches();
                            }
                        });
                        subscriptionView.showSnackbarMessage(String.valueOf(realm.where(LocationSwitch.class).equalTo("id", locationSwitchId).findFirst().isSubscribed()));
                    }
                });
                subscriptionView.addViewToCustomList(checkbox);
                subscriptionView.addViewToLocationView(checkbox);
            }
        }
    }

    @Override
    public void checkboxSelected(AppCompatCheckBox view) {
        Realm realm = Realm.getDefaultInstance();
        final boolean checked = view.isChecked();
        switch (view.getId()) {
            case R.id.arianespace_switch: {
                final AgencySwitch AgencySwitchAriane = realm.where(AgencySwitch.class)
                        .equalTo("id", Constants.AGENCY_ARIANESPACE)
                        .findFirst();

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        AgencySwitchAriane.setSubscribed(checked);

                    }
                });
                subscriptionView.updateCheckboxByID(AgencySwitchAriane.getName().substring(0, Math.min(AgencySwitchAriane.getName().length(), 5)).hashCode(), AgencySwitchAriane.isSubscribed());
                break;
            }
            case R.id.casc_switch: {
                final LocationSwitch locationSwitchOne = realm.where(LocationSwitch.class)
                        .equalTo("id", Constants.LOCATION_CASC[0])
                        .findFirst();

                final LocationSwitch locationSwitchTwo = realm.where(LocationSwitch.class)
                        .equalTo("id", Constants.LOCATION_CASC[1])
                        .findFirst();

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        locationSwitchOne.setSubscribed(checked);
                        locationSwitchTwo.setSubscribed(checked);
                    }
                });

                subscriptionView.updateCheckboxByID(locationSwitchOne.getName().substring(0, Math.min(locationSwitchOne.getName().length(), 5)).hashCode(), locationSwitchOne.isSubscribed());
                subscriptionView.updateCheckboxByID(locationSwitchTwo.getName().substring(0, Math.min(locationSwitchTwo.getName().length(), 5)).hashCode(), locationSwitchTwo.isSubscribed());
                break;
            }
            case R.id.isro_switch: {
                final AgencySwitch AgencySwitch = realm.where(AgencySwitch.class)
                        .equalTo("id", Constants.AGENCY_ISRO)
                        .findFirst();

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        AgencySwitch.setSubscribed(checked);
                    }
                });

                subscriptionView.updateCheckboxByID(AgencySwitch.getName().substring(0, Math.min(AgencySwitch.getName().length(), 5)).hashCode(), AgencySwitch.isSubscribed());
                break;
            }
            case R.id.cape_switch: {
                final LocationSwitch locationSwitch = realm.where(LocationSwitch.class)
                        .equalTo("id", Constants.LOCATION_CAPE)
                        .findFirst();

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        locationSwitch.setSubscribed(checked);
                    }
                });

                subscriptionView.updateCheckboxByID(locationSwitch.getName().substring(0, Math.min(locationSwitch.getName().length(), 5)).hashCode(), locationSwitch.isSubscribed());
                break;
            }
            case R.id.nasa_switch: {
                final AgencySwitch AgencySwitch = realm.where(AgencySwitch.class)
                        .equalTo("id", Constants.AGENCY_NASA)
                        .findFirst();

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        AgencySwitch.setSubscribed(checked);
                    }
                });

                subscriptionView.updateCheckboxByID(AgencySwitch.getName().substring(0, Math.min(AgencySwitch.getName().length(), 5)).hashCode(), AgencySwitch.isSubscribed());
                break;
            }
            case R.id.spacex_switch: {
                final AgencySwitch AgencySwitch = realm.where(AgencySwitch.class)
                        .equalTo("id", Constants.AGENCY_SPACEX)
                        .findFirst();

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        AgencySwitch.setSubscribed(checked);
                    }
                });

                subscriptionView.updateCheckboxByID(AgencySwitch.getName().substring(0, Math.min(AgencySwitch.getName().length(), 5)).hashCode(), AgencySwitch.isSubscribed());
                break;
            }
            case R.id.roscosmos_switch: {
                final AgencySwitch AgencySwitchOne = realm.where(AgencySwitch.class)
                        .equalTo("id", Constants.AGENCY_ROSCOSMOS[0])
                        .findFirst();

                final AgencySwitch AgencySwitchTwo = realm.where(AgencySwitch.class)
                        .equalTo("id", Constants.AGENCY_ROSCOSMOS[1])
                        .findFirst();

                final AgencySwitch AgencySwitchThree = realm.where(AgencySwitch.class)
                        .equalTo("id", Constants.AGENCY_ROSCOSMOS[2])
                        .findFirst();

                final AgencySwitch AgencySwitchFour = realm.where(AgencySwitch.class)
                        .equalTo("id", Constants.AGENCY_ROSCOSMOS[3])
                        .findFirst();

                final LocationSwitch LocationSwitchOne = realm.where(LocationSwitch.class)
                        .equalTo("id", Constants.LOCATION_ROSCOSMOS[0])
                        .findFirst();

                final LocationSwitch LocationSwitchTwo = realm.where(LocationSwitch.class)
                        .equalTo("id", Constants.LOCATION_ROSCOSMOS[1])
                        .findFirst();

                final LocationSwitch LocationSwitchThree = realm.where(LocationSwitch.class)
                        .equalTo("id", Constants.LOCATION_ROSCOSMOS[2])
                        .findFirst();

                final LocationSwitch LocationSwitchFour = realm.where(LocationSwitch.class)
                        .equalTo("id", Constants.LOCATION_ROSCOSMOS[3])
                        .findFirst();

                final LocationSwitch LocationSwitchFive = realm.where(LocationSwitch.class)
                        .equalTo("id", Constants.LOCATION_ROSCOSMOS[4])
                        .findFirst();

                final LocationSwitch LocationSwitchSix = realm.where(LocationSwitch.class)
                        .equalTo("id", Constants.LOCATION_ROSCOSMOS[5])
                        .findFirst();

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        AgencySwitchOne.setSubscribed(checked);
                        AgencySwitchTwo.setSubscribed(checked);
                        AgencySwitchThree.setSubscribed(checked);
                        AgencySwitchFour.setSubscribed(checked);

                        LocationSwitchOne.setSubscribed(checked);
                        LocationSwitchTwo.setSubscribed(checked);
                        LocationSwitchThree.setSubscribed(checked);
                        LocationSwitchFour.setSubscribed(checked);
                        LocationSwitchFive.setSubscribed(checked);
                        LocationSwitchSix.setSubscribed(checked);
                    }
                });
                subscriptionView.updateCheckboxByID(AgencySwitchOne.getName().substring(0, Math.min(AgencySwitchOne.getName().length(), 5)).hashCode(), AgencySwitchOne.isSubscribed());
                subscriptionView.updateCheckboxByID(AgencySwitchTwo.getName().substring(0, Math.min(AgencySwitchTwo.getName().length(), 5)).hashCode(), AgencySwitchTwo.isSubscribed());
                subscriptionView.updateCheckboxByID(AgencySwitchThree.getName().substring(0, Math.min(AgencySwitchThree.getName().length(), 5)).hashCode(), AgencySwitchThree.isSubscribed());
                subscriptionView.updateCheckboxByID(AgencySwitchFour.getName().substring(0, Math.min(AgencySwitchFour.getName().length(), 5)).hashCode(), AgencySwitchFour.isSubscribed());

                subscriptionView.updateCheckboxByID(LocationSwitchOne.getName().substring(0, Math.min(LocationSwitchOne.getName().length(), 5)).hashCode(), LocationSwitchOne.isSubscribed());
                subscriptionView.updateCheckboxByID(LocationSwitchTwo.getName().substring(0, Math.min(LocationSwitchTwo.getName().length(), 5)).hashCode(), LocationSwitchTwo.isSubscribed());
                subscriptionView.updateCheckboxByID(LocationSwitchThree.getName().substring(0, Math.min(LocationSwitchThree.getName().length(), 5)).hashCode(), LocationSwitchThree.isSubscribed());
                subscriptionView.updateCheckboxByID(LocationSwitchFour.getName().substring(0, Math.min(LocationSwitchFour.getName().length(), 5)).hashCode(), LocationSwitchFour.isSubscribed());
                subscriptionView.updateCheckboxByID(LocationSwitchFive.getName().substring(0, Math.min(LocationSwitchFive.getName().length(), 5)).hashCode(), LocationSwitchFive.isSubscribed());
                subscriptionView.updateCheckboxByID(LocationSwitchSix.getName().substring(0, Math.min(LocationSwitchSix.getName().length(), 5)).hashCode(), LocationSwitchSix.isSubscribed());
                break;
            }
            case R.id.ula_switch: {
                final AgencySwitch AgencySwitch = realm.where(AgencySwitch.class)
                        .equalTo("id", Constants.AGENCY_ULA)
                        .findFirst();

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        AgencySwitch.setSubscribed(checked);
                    }
                });

                subscriptionView.updateCheckboxByID(AgencySwitch.getName().substring(0, Math.min(AgencySwitch.getName().length(), 5)).hashCode(), AgencySwitch.isSubscribed());
                break;
            }
            case R.id.KSC_switch: {
                final LocationSwitch LocationSwitch = realm.where(LocationSwitch.class)
                        .equalTo("id", Constants.LOCATION_KSC)
                        .findFirst();

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        LocationSwitch.setSubscribed(checked);
                    }
                });

                subscriptionView.updateCheckboxByID(LocationSwitch.getName().substring(0, Math.min(LocationSwitch.getName().length(), 5)).hashCode(), LocationSwitch.isSubscribed());
                break;
            }
            case R.id.van_switch: {
                final LocationSwitch LocationSwitch = realm.where(LocationSwitch.class)
                        .equalTo("id", Constants.LOCATION_VAN)
                        .findFirst();

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        LocationSwitch.setSubscribed(checked);
                    }
                });

                subscriptionView.updateCheckboxByID(LocationSwitch.getName().substring(0, Math.min(LocationSwitch.getName().length(), 5)).hashCode(), LocationSwitch.isSubscribed());
                break;
            }
            case R.id.orbital_switch: {
                final AgencySwitch AgencySwitch = realm.where(AgencySwitch.class)
                        .equalTo("id", Constants.AGENCY_ORBITAL_ATK)
                        .findFirst();

                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        AgencySwitch.setSubscribed(checked);
                    }
                });

                subscriptionView.updateCheckboxByID(AgencySwitch.getName().substring(0, Math.min(AgencySwitch.getName().length(), 5)).hashCode(), AgencySwitch.isSubscribed());
                break;
            }
        }

        syncSwitchStateAsync(realm);
    }

    private void syncSwitchStateAsync(Realm realm) {
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                RealmResults<LocationSwitch> locationSwitches = realm.where(LocationSwitch.class).findAll();
                RealmResults<AgencySwitch> agencySwitches = realm.where(AgencySwitch.class).findAll();

                for (LocationSwitch locationSwitch : locationSwitches) {
                    Location location = realm.where(Location.class).equalTo("id", locationSwitch.getId()).findFirst();
                    if (location != null) {
                        location.setSubscribed(locationSwitch.isSubscribed());
                        realm.copyToRealmOrUpdate(location);
                    }
                }

                for (AgencySwitch agencySwitch : agencySwitches) {
                    Agency agency = realm.where(Agency.class).equalTo("id", agencySwitch.getId()).findFirst();
                    if (agency != null) {
                        agency.setSubscribed(agencySwitch.isSubscribed());
                        realm.copyToRealmOrUpdate(agency);
                    }
                }
            }
        });
    }

    @Override
    public void start() {

    }
}
