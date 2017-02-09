package me.calebjones.spacelaunchnow.content.services;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import me.calebjones.spacelaunchnow.BuildConfig;
import me.calebjones.spacelaunchnow.R;
import me.calebjones.spacelaunchnow.content.database.ListPreferences;
import me.calebjones.spacelaunchnow.content.database.SwitchPreferences;
import me.calebjones.spacelaunchnow.content.jobs.UpdateJob;
import me.calebjones.spacelaunchnow.content.models.Constants;
import me.calebjones.spacelaunchnow.content.util.QueryBuilder;
import me.calebjones.spacelaunchnow.data.models.realm.Agency;
import me.calebjones.spacelaunchnow.data.models.realm.AgencySwitch;
import me.calebjones.spacelaunchnow.data.models.realm.Launch;
import me.calebjones.spacelaunchnow.data.models.realm.LaunchNotification;
import me.calebjones.spacelaunchnow.data.models.realm.Location;
import me.calebjones.spacelaunchnow.data.models.realm.LocationSwitch;
import me.calebjones.spacelaunchnow.data.models.realm.UpdateRecord;
import me.calebjones.spacelaunchnow.data.networking.interfaces.LibraryRequestInterface;
import me.calebjones.spacelaunchnow.data.networking.responses.launchlibrary.LaunchResponse;
import me.calebjones.spacelaunchnow.utils.Connectivity;
import me.calebjones.spacelaunchnow.utils.FileUtils;
import me.calebjones.spacelaunchnow.utils.Utils;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class LaunchDataService extends BaseService {

    public LaunchDataService() {
        super("LaunchDataService");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        listPreference = ListPreferences.getInstance(getApplicationContext());
        switchPreferences = SwitchPreferences.getInstance(getApplicationContext());
        this.sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return super.onStartCommand(intent, flags, startId);
    }

    public static void startActionSyncNotifiers(Context context) {
        Intent intent = new Intent(context, LaunchDataService.class);
        intent.setAction(Constants.SYNC_NOTIFIERS);
        context.startService(intent);
        Timber.v("Sending Delete intent.");
    }

    public static void startActionUpdateNextLaunch(Context context) {
        Intent intent = new Intent(context, LaunchDataService.class);
        intent.setAction(Constants.ACTION_UPDATE_NEXT_LAUNCH);
        context.startService(intent);
        Timber.v("Sending Delete intent.");
    }

    public static void startActionBackground(Context context) {
        Intent intent = new Intent(context, LaunchDataService.class);
        intent.setAction(Constants.ACTION_UPDATE_BACKGROUND);
        context.startService(intent);
        Timber.v("Sending Delete intent.");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            listPreference.isUpdating(true);
            Timber.d("LaunchDataService - Intent received:  %s ", intent.getAction());
            String action = intent.getAction();

            // Create a new empty instance of Realm
            mRealm = Realm.getDefaultInstance();

            if (BuildConfig.DEBUG) {

                Date now = new Date();
                int id = Integer.parseInt(new SimpleDateFormat("ddHHmmss", Locale.US).format(now));

                NotificationCompat.Builder mBuilder = new NotificationCompat
                        .Builder(getApplicationContext());
                NotificationManager mNotifyManager = (NotificationManager) getApplicationContext()
                        .getSystemService(Context.NOTIFICATION_SERVICE);

                String msg = "Launch Data - Intent received - " + action;
                mBuilder.setContentTitle("Scheduling Update - ")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setSmallIcon(R.drawable.ic_rocket_white)
                        .setContentText(msg);
                mNotifyManager.notify(id, mBuilder.build());
            }

            //Usually called on first launch
            if (Constants.ACTION_GET_ALL_DATA.equals(action)) {
                Timber.v("Intent action received: %s", action);
                if (this.sharedPref.getBoolean("background", true)) {
                    scheduleLaunchUpdates();
                }

                if (getUpcomingLaunchesAll(this)) {
                    if (getLaunchesByDate("1950-01-01", Utils.getEndDate(this), this)) {
                        Intent rocketIntent = new Intent(getApplicationContext(), VehicleDataService.class);
                        rocketIntent.setAction(Constants.ACTION_GET_VEHICLES_DETAIL);
                        startService(rocketIntent);


                        Intent libraryIntent = new Intent(getApplicationContext(), LibraryDataService.class);
                        libraryIntent.setAction(Constants.ACTION_GET_ALL_DATA);
                        startService(libraryIntent);

                        startService(new Intent(this, NextLaunchTracker.class));
                    }
                }
            } else if (Constants.ACTION_UPDATE_LAUNCH.equals(action)) {
                int id = intent.getIntExtra("launchID", 0);
                if (id > 0) {
                    Timber.v("Updating launch id: %s", id);
                    getLaunchById(id, this);
                }
                syncNotifiers(this);

                // Called from NextLaunchFragment
            } else if (Constants.ACTION_GET_UP_LAUNCHES.equals(action)) {

                Timber.v("Intent action received: %s", action);
                if (this.sharedPref.getBoolean("background", true)) {
                    scheduleLaunchUpdates();
                }

                getUpcomingLaunches(this);
                syncNotifiers(this);
                this.startService(new Intent(this, NextLaunchTracker.class));

                // Called from PrevLaunchFragment
            } else if (Constants.ACTION_GET_PREV_LAUNCHES.equals(action)) {

                Timber.v("Intent action received: %s", action);
                if (intent.getStringExtra("startDate") != null && intent.getStringExtra("endDate") != null) {
                    getLaunchesByDate(intent.getStringExtra("startDate"), intent.getStringExtra("endDate"), this);
                } else {
                    getLaunchesByDate("1950-01-01", Utils.getEndDate(this), this);
                }

            } else if (Constants.ACTION_UPDATE_NEXT_LAUNCH.equals(action)) {

                Timber.v("Intent action received: %s", action);
                getNextLaunches(this);

            } else if (Constants.SYNC_NOTIFIERS.equals(action)) {

                Timber.v("Intent action received: %s", action);
                syncNotifiers(this);

            } else if (Constants.ACTION_UPDATE_BACKGROUND.equals(action)) {

                Timber.v("Intent action received: %s", action);
                syncBackground(this);

            }
            listPreference.isUpdating(false);
        } else {

            Timber.e("LaunchDataService - onHandleIntent: ERROR - Unknown Intent");

        }

        syncSwitchState(mRealm);
        Timber.v("Finished!");
        mRealm.close();
    }

    public static boolean syncBackground(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        boolean wifiOnly = sharedPref.getBoolean("wifi_only", false);
        boolean dataSaver = sharedPref.getBoolean("data_saver", false);
        boolean wifiConnected = Connectivity.isConnectedWifi(context);
        boolean success;

        if (wifiOnly) {
            if (wifiConnected) {
                success = getUpcomingLaunches(context);
                syncNotifiers(context);
                checkFullSync(context);

            } else {
                success = false;
            }
        } else if (dataSaver && !wifiConnected) {

            success = getNextLaunches(context);

        } else {

            success = getUpcomingLaunches(context);
            checkFullSync(context);

        }
        return success;
    }

    private static void syncSwitchState(Realm realm) {
        RealmResults<LocationSwitch> locationSwitches = realm.where(LocationSwitch.class).findAll();
        RealmResults<AgencySwitch> agencySwitches = realm.where(AgencySwitch.class).findAll();

        for (LocationSwitch locationSwitch : locationSwitches) {
            Location location = realm.where(Location.class).equalTo("id", locationSwitch.getId()).findFirst();
            if (location != null) {
                realm.beginTransaction();
                location.setSubscribed(locationSwitch.isSubscribed());
                realm.copyToRealmOrUpdate(location);
                realm.commitTransaction();
            }
        }

        for (AgencySwitch agencySwitch : agencySwitches) {
            Agency agency = realm.where(Agency.class).equalTo("id", agencySwitch.getId()).findFirst();
            if (agency != null) {
                realm.beginTransaction();
                agency.setSubscribed(agencySwitch.isSubscribed());
                realm.copyToRealmOrUpdate(agency);
                realm.commitTransaction();
            }
        }
    }

    private static void checkFullSync(Context context) {
        Realm realm = Realm.getDefaultInstance();
        checkUpcomingLaunches(context, realm);
        checkPreviousLaunches(context, realm);
        checkMissions(context, realm);
        checkVehicles(context, realm);
    }

    private static void checkUpcomingLaunches(Context context, Realm realm) {
        UpdateRecord record = realm.where(UpdateRecord.class).equalTo("type", Constants.ACTION_GET_UP_LAUNCHES).findFirst();
        if (record != null){
            Date currentDate = new Date();
            Date lastUpdateDate = record.getDate();
            long timeSinceUpdate = currentDate.getTime() - lastUpdateDate.getTime();
            long daysMaxUpdate = 2592000000L;
            if (timeSinceUpdate > daysMaxUpdate) {
                getUpcomingLaunchesAll(context);
            }
        } else {
            getUpcomingLaunchesAll(context);
        }
    }

    private static void checkMissions(Context context, Realm realm) {
        UpdateRecord record = realm.where(UpdateRecord.class).equalTo("type", Constants.ACTION_GET_MISSION).findFirst();
        if (record != null){
            Date currentDate = new Date();
            Date lastUpdateDate = record.getDate();
            long timeSinceUpdate = currentDate.getTime() - lastUpdateDate.getTime();
            long daysMaxUpdate = 2592000000L;
            if (timeSinceUpdate > daysMaxUpdate) {
                context.startService(new Intent(context, MissionDataService.class));
            }
        } else {
            context.startService(new Intent(context, MissionDataService.class));
        }
    }

    private static void checkPreviousLaunches(Context context, Realm realm) {
        UpdateRecord record = realm.where(UpdateRecord.class).equalTo("type", Constants.ACTION_GET_PREV_LAUNCHES).findFirst();
        if (record != null){
            Date currentDate = new Date();
            Date lastUpdateDate = record.getDate();
            long timeSinceUpdate = currentDate.getTime() - lastUpdateDate.getTime();
            long daysMaxUpdate = 2592000000L;
            if (timeSinceUpdate > daysMaxUpdate) {
                getLaunchesByDate("1950-01-01", Utils.getEndDate(context), context);
            }
        } else {
            getLaunchesByDate("1950-01-01", Utils.getEndDate(context), context);
        }
    }

    private static void checkVehicles(Context context, Realm realm) {
        UpdateRecord record = realm.where(UpdateRecord.class).equalTo("type", Constants.ACTION_GET_VEHICLES_DETAIL).findFirst();
        if (record != null){
            Date currentDate = new Date();
            Date lastUpdateDate = record.getDate();
            long timeSinceUpdate = currentDate.getTime() - lastUpdateDate.getTime();
            long daysMaxUpdate = 2592000000L;
            if (timeSinceUpdate > daysMaxUpdate) {
                Intent rocketIntent = new Intent(context, VehicleDataService.class);
                rocketIntent.setAction(Constants.ACTION_GET_VEHICLES_DETAIL);
                context.startService(rocketIntent);
            }
        } else {
            Intent rocketIntent = new Intent(context, VehicleDataService.class);
            rocketIntent.setAction(Constants.ACTION_GET_VEHICLES_DETAIL);
            context.startService(rocketIntent);
        }
    }

    private static void syncNotifiers(Context context) {
        RealmResults<Launch> launchRealms;
        Date date = new Date();

        SwitchPreferences switchPreferences = SwitchPreferences.getInstance(context);
        Realm mRealm = Realm.getDefaultInstance();

        launchRealms = QueryBuilder.buildSwitchQuery(mRealm);

        for (final Launch launchrealm : launchRealms) {
            if (!launchrealm.isUserToggledNotifiable() && !launchrealm.isNotifiable()) {
                mRealm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        launchrealm.setNotifiable(true);
                    }
                });
            }
        }
    }

    private static boolean getLaunchesByDate(String startDate, String endDate, Context context) {
        LibraryRequestInterface request = getRetrofit().create(LibraryRequestInterface.class);
        Call<LaunchResponse> call;
        Response<LaunchResponse> launchResponse;
        RealmList<Launch> items = new RealmList<>();

        Realm mRealm = Realm.getDefaultInstance();

        ListPreferences listPreference = ListPreferences.getInstance(context);

        int offset = 0;
        int total = 10;
        int count;

        try {
            while (total != offset) {
                if (listPreference.isDebugEnabled()) {
                    call = request.getDebugLaunchesByDate(startDate, endDate, offset);
                } else {
                    call = request.getLaunchesByDate(startDate, endDate, offset);
                }
                launchResponse = call.execute();
                if (launchResponse.isSuccessful()) {
                    total = launchResponse.body().getTotal();
                    count = launchResponse.body().getCount();
                    offset = offset + count;
                    Timber.v("LaunchesByDate Count: %s", offset);
                    Collections.addAll(items, launchResponse.body().getLaunches());
                } else {
                    throw new IOException(launchResponse.errorBody().string());
                }
            }
            mRealm.beginTransaction();
            mRealm.copyToRealmOrUpdate(items);
            mRealm.commitTransaction();

            mRealm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    UpdateRecord updateRecord = new UpdateRecord();
                    updateRecord.setType(Constants.ACTION_GET_PREV_LAUNCHES);
                    updateRecord.setDate(new Date());
                    updateRecord.setSuccessful(true);
                    realm.copyToRealmOrUpdate(updateRecord);
                }
            });

            Timber.v("Success!");
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(Constants.ACTION_SUCCESS_PREV_LAUNCHES);
            context.sendBroadcast(broadcastIntent);
            return true;

        } catch (IOException e) {
            Timber.e("Error: %s", e.getLocalizedMessage());

            mRealm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    UpdateRecord updateRecord = new UpdateRecord();
                    updateRecord.setType(Constants.ACTION_GET_PREV_LAUNCHES);
                    updateRecord.setDate(new Date());
                    updateRecord.setSuccessful(false);
                    realm.copyToRealmOrUpdate(updateRecord);
                }
            });

            Intent broadcastIntent = new Intent();
            broadcastIntent.putExtra("error", e.getLocalizedMessage());
            broadcastIntent.setAction(Constants.ACTION_FAILURE_PREV_LAUNCHES);
            context.sendBroadcast(broadcastIntent);
            return false;
        }
    }

    private static boolean getUpcomingLaunches(Context context) {
        LibraryRequestInterface request = getRetrofit().create(LibraryRequestInterface.class);
        Call<LaunchResponse> call;
        Response<LaunchResponse> launchResponse;
        RealmList<Launch> items = new RealmList<>();

        Realm mRealm = Realm.getDefaultInstance();

        ListPreferences listPreference = ListPreferences.getInstance(context);

        int offset = 0;
        int total = 10;
        int count;

        try {
            while (total != offset) {
                if (listPreference.isDebugEnabled()) {
                    call = request.getDebugUpcomingLaunches(offset);
                } else {
                    call = request.getUpcomingLaunches(offset);
                }
                launchResponse = call.execute();
                if (launchResponse.isSuccessful()) {
                    total = launchResponse.body().getTotal();
                    count = launchResponse.body().getCount();
                    offset = offset + count;
                    Timber.v("UpcomingLaunches Count: %s", offset);
                    Collections.addAll(items, launchResponse.body().getLaunches());
                } else {
                    throw new IOException(launchResponse.errorBody().string());
                }
            }
            for (Launch item : items) {
                mRealm.beginTransaction();
                Launch previous = mRealm.where(Launch.class)
                        .equalTo("id", item.getId())
                        .findFirst();
                if (previous != null) {
                    if ((!previous.getNet().equals(item.getNet()) || (previous.getStatus().intValue() != item.getStatus().intValue()))) {
                        Timber.v("%s status has changed.", item.getName());
                        LaunchNotification notification = mRealm.where(LaunchNotification.class).equalTo("id", item.getId()).findFirst();
                        if (notification != null) {
                            notification.resetNotifiers();
                            mRealm.copyToRealmOrUpdate(notification);
                        }
                    }
                    Timber.v("UpcomingLaunches updating items: %s", previous.getName());
                    item.setEventID(previous.getEventID());
                    item.setSyncCalendar(previous.syncCalendar());
                    item.setLaunchTimeStamp(previous.getLaunchTimeStamp());
                }
                mRealm.copyToRealmOrUpdate(item);
                mRealm.commitTransaction();
            }

            mRealm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    UpdateRecord updateRecord = new UpdateRecord();
                    updateRecord.setType(Constants.ACTION_UPDATE_NEXT_LAUNCH);
                    updateRecord.setDate(new Date());
                    updateRecord.setSuccessful(true);
                    realm.copyToRealmOrUpdate(updateRecord);
                }
            });

            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(Constants.ACTION_SUCCESS_UP_LAUNCHES);
            context.getApplicationContext().sendBroadcast(broadcastIntent);

            mRealm.close();
            FileUtils.saveSuccess(true, Constants.ACTION_UPDATE_NEXT_LAUNCH, context);
            return true;
        } catch (IOException e) {
            Timber.e("Error: %s", e.getLocalizedMessage());

            mRealm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    UpdateRecord updateRecord = new UpdateRecord();
                    updateRecord.setType(Constants.ACTION_UPDATE_NEXT_LAUNCH);
                    updateRecord.setDate(new Date());
                    updateRecord.setSuccessful(false);
                    realm.copyToRealmOrUpdate(updateRecord);
                }
            });

            Intent broadcastIntent = new Intent();
            broadcastIntent.putExtra("error", e.getLocalizedMessage());
            broadcastIntent.setAction(Constants.ACTION_FAILURE_UP_LAUNCHES);
            context.getApplicationContext().sendBroadcast(broadcastIntent);
            mRealm.close();
            FileUtils.saveSuccess(false, Constants.ACTION_UPDATE_NEXT_LAUNCH + " " + e.getLocalizedMessage(), context);
            return false;
        }
    }

    private static boolean getUpcomingLaunchesAll(Context context) {
        LibraryRequestInterface request = getRetrofit().create(LibraryRequestInterface.class);
        Call<LaunchResponse> call;
        Response<LaunchResponse> launchResponse;
        RealmList<Launch> items = new RealmList<>();

        Realm mRealm = Realm.getDefaultInstance();

        ListPreferences listPreference = ListPreferences.getInstance(context);

        int offset = 0;
        int total = 10;
        int count;

        try {
            while (total != offset) {
                if (listPreference.isDebugEnabled()) {
                    call = request.getDebugUpcomingLaunchesAll(offset);
                } else {
                    call = request.getUpcomingLaunchesAll(offset);
                }
                launchResponse = call.execute();
                if (launchResponse.isSuccessful()) {
                    total = launchResponse.body().getTotal();
                    count = launchResponse.body().getCount();
                    offset = offset + count;
                    Timber.v("UpcomingLaunches Count: %s", offset);
                    Collections.addAll(items, launchResponse.body().getLaunches());
                } else {
                    throw new IOException(launchResponse.errorBody().string());
                }
            }
            for (Launch item : items) {
                Launch previous = mRealm.where(Launch.class)
                        .equalTo("id", item.getId())
                        .findFirst();
                if (previous != null) {
                    Timber.v("UpcomingLaunches updating items: %s", previous.getName());
                    item.setEventID(previous.getEventID());
                    item.setSyncCalendar(previous.syncCalendar());
                    item.setLaunchTimeStamp(previous.getLaunchTimeStamp());
                    item.setIsNotifiedDay(previous.getIsNotifiedDay());
                    item.setIsNotifiedHour(previous.getIsNotifiedHour());
                    item.setIsNotifiedTenMinute(previous.getIsNotifiedTenMinute());
                }
                mRealm.beginTransaction();
                mRealm.copyToRealmOrUpdate(item);
                mRealm.commitTransaction();
            }

            mRealm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    UpdateRecord updateRecord = new UpdateRecord();
                    updateRecord.setType(Constants.ACTION_GET_UP_LAUNCHES);
                    updateRecord.setDate(new Date());
                    updateRecord.setSuccessful(true);
                    realm.copyToRealmOrUpdate(updateRecord);
                }
            });

            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(Constants.ACTION_SUCCESS_UP_LAUNCHES);
            context.getApplicationContext().sendBroadcast(broadcastIntent);

            mRealm.close();
            FileUtils.saveSuccess(true, Constants.ACTION_SUCCESS_UP_LAUNCHES, context);
            return true;
        } catch (IOException e) {
            Timber.e("Error: %s", e.getLocalizedMessage());

            mRealm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    UpdateRecord updateRecord = new UpdateRecord();
                    updateRecord.setType(Constants.ACTION_GET_UP_LAUNCHES);
                    updateRecord.setDate(new Date());
                    updateRecord.setSuccessful(false);
                    realm.copyToRealmOrUpdate(updateRecord);
                }
            });

            Intent broadcastIntent = new Intent();
            broadcastIntent.putExtra("error", e.getLocalizedMessage());
            broadcastIntent.setAction(Constants.ACTION_FAILURE_UP_LAUNCHES);
            context.getApplicationContext().sendBroadcast(broadcastIntent);
            mRealm.close();
            FileUtils.saveSuccess(false, Constants.ACTION_SUCCESS_UP_LAUNCHES + " " + e.getLocalizedMessage(), context);
            return false;
        }
    }

    public static boolean getNextLaunches(Context context) {
        LibraryRequestInterface request = getRetrofit().create(LibraryRequestInterface.class);
        Call<LaunchResponse> call;
        Response<LaunchResponse> launchResponse;
        RealmList<Launch> items = new RealmList<>();

        Realm mRealm = Realm.getDefaultInstance();

        ListPreferences listPreference = ListPreferences.getInstance(context);

        try {
            if (listPreference.isDebugEnabled()) {
                call = request.getDebugMiniNextLaunch();
            } else {
                call = request.getMiniNextLaunch();
            }
            launchResponse = call.execute();
            if (launchResponse.isSuccessful()) {
                Collections.addAll(items, launchResponse.body().getLaunches());
            } else {
                throw new IOException();
            }
            for (Launch item : items) {
                Launch previous = mRealm.where(Launch.class)
                        .equalTo("id", item.getId())
                        .findFirst();
                if (previous != null) {
                    if ((!previous.getNet().equals(item.getNet()) || (previous.getStatus().intValue() != item.getStatus().intValue()))) {
                        Timber.v("%s status has changed.", item.getName());
                        LaunchNotification notification = mRealm.where(LaunchNotification.class).equalTo("id", item.getId()).findFirst();
                        mRealm.beginTransaction();
                        if (notification != null) {
                            notification.resetNotifiers();
                            mRealm.copyToRealmOrUpdate(notification);
                        }
                        previous.resetNotifiers();
                        mRealm.copyToRealmOrUpdate(previous);
                        mRealm.commitTransaction();
                        getLaunchById(item.getId(), context);
                    }
                }
            }

            mRealm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    UpdateRecord updateRecord = new UpdateRecord();
                    updateRecord.setType(Constants.ACTION_UPDATE_UP_LAUNCHES);
                    updateRecord.setDate(new Date());
                    updateRecord.setSuccessful(true);
                    realm.copyToRealmOrUpdate(updateRecord);
                }
            });

            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(Constants.ACTION_SUCCESS_UP_LAUNCHES);
            context.sendBroadcast(broadcastIntent);

            syncNotifiers(context);
            context.startService(new Intent(context, NextLaunchTracker.class));

            mRealm.close();
            FileUtils.saveSuccess(true, Constants.ACTION_UPDATE_UP_LAUNCHES, context);
            return true;
        } catch (IOException e) {
            Timber.e("Error: %s", e.getLocalizedMessage());

            mRealm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    UpdateRecord updateRecord = new UpdateRecord();
                    updateRecord.setType(Constants.ACTION_UPDATE_UP_LAUNCHES);
                    updateRecord.setDate(new Date());
                    updateRecord.setSuccessful(false);
                    realm.copyToRealmOrUpdate(updateRecord);
                }
            });

            Intent broadcastIntent = new Intent();
            broadcastIntent.putExtra("error", e.getLocalizedMessage());
            broadcastIntent.setAction(Constants.ACTION_FAILURE_UP_LAUNCHES);
            context.sendBroadcast(broadcastIntent);

            syncNotifiers(context);
            context.startService(new Intent(context, NextLaunchTracker.class));

            mRealm.close();
            FileUtils.saveSuccess(false, Constants.ACTION_UPDATE_UP_LAUNCHES + " " + e.getLocalizedMessage(), context);
            return false;
        }
    }

    private static boolean getLaunchById(int id, Context context) {
        LibraryRequestInterface request = getRetrofit().create(LibraryRequestInterface.class);
        Call<LaunchResponse> call;

        Realm mRealm = Realm.getDefaultInstance();

        ListPreferences listPreference = ListPreferences.getInstance(context);

        if (listPreference.isDebugEnabled()) {
            call = request.getDebugLaunchByID(id);
        } else {
            call = request.getLaunchByID(id);
        }

        Response<LaunchResponse> launchResponse;
        try {
            launchResponse = call.execute();
            if (launchResponse.isSuccessful()) {
                RealmList<Launch> items = new RealmList<>(launchResponse.body().getLaunches());
                for (Launch item : items) {
                    Launch previous = mRealm.where(Launch.class)
                            .equalTo("id", item.getId())
                            .findFirst();
                    if (previous != null) {
                        item.setEventID(previous.getEventID());
                        item.setSyncCalendar(previous.syncCalendar());
                        item.setLaunchTimeStamp(previous.getLaunchTimeStamp());
                        item.setIsNotifiedDay(previous.getIsNotifiedDay());
                        item.setIsNotifiedHour(previous.getIsNotifiedHour());
                        item.setIsNotifiedTenMinute(previous.getIsNotifiedTenMinute());
                    }
                    mRealm.beginTransaction();
                    mRealm.copyToRealmOrUpdate(item);
                    mRealm.commitTransaction();
                    Timber.v("Updated launch: %s", item.getId());
                }
            }
            mRealm.close();
            FileUtils.saveSuccess(true, Constants.ACTION_SUCCESS_LAUNCH, context);
            return true;
        } catch (IOException e) {
            Timber.e("Error: %s", e.getLocalizedMessage());
            mRealm.close();
            FileUtils.saveSuccess(false, Constants.ACTION_SUCCESS_LAUNCH, context);
            return false;
        }
    }

    public void scheduleLaunchUpdates() {
        Timber.d("LaunchDataService - scheduleLaunchUpdates");
        UpdateJob.scheduleJob(this);
    }
}
