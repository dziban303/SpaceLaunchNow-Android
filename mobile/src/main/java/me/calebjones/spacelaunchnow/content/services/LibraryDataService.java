package me.calebjones.spacelaunchnow.content.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;
import me.calebjones.spacelaunchnow.content.database.ListPreferences;
import me.calebjones.spacelaunchnow.content.models.Constants;
import me.calebjones.spacelaunchnow.data.models.realm.Agency;
import me.calebjones.spacelaunchnow.data.models.realm.AgencySwitch;
import me.calebjones.spacelaunchnow.data.models.realm.Location;
import me.calebjones.spacelaunchnow.data.models.realm.LocationSwitch;
import me.calebjones.spacelaunchnow.data.models.realm.Mission;
import me.calebjones.spacelaunchnow.data.models.realm.Pad;
import me.calebjones.spacelaunchnow.data.models.realm.RealmStr;
import me.calebjones.spacelaunchnow.data.models.realm.Rocket;
import me.calebjones.spacelaunchnow.data.models.realm.RocketDetails;
import me.calebjones.spacelaunchnow.data.models.realm.RocketFamily;
import me.calebjones.spacelaunchnow.data.networking.interfaces.APIRequestInterface;
import me.calebjones.spacelaunchnow.data.networking.interfaces.LibraryRequestInterface;
import me.calebjones.spacelaunchnow.data.networking.responses.base.VehicleResponse;
import me.calebjones.spacelaunchnow.data.networking.responses.launchlibrary.AgencyResponse;
import me.calebjones.spacelaunchnow.data.networking.responses.launchlibrary.AgencySwitchResponse;
import me.calebjones.spacelaunchnow.data.networking.responses.launchlibrary.LocationResponse;
import me.calebjones.spacelaunchnow.data.networking.responses.launchlibrary.LocationSwitchResponse;
import me.calebjones.spacelaunchnow.data.networking.responses.launchlibrary.MissionResponse;
import me.calebjones.spacelaunchnow.data.networking.responses.launchlibrary.PadResponse;
import me.calebjones.spacelaunchnow.data.networking.responses.launchlibrary.RocketFamilyResponse;
import me.calebjones.spacelaunchnow.data.networking.responses.launchlibrary.RocketResponse;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;


/**
 * This grabs rockets from LaunchLibrary
 */
public class LibraryDataService extends IntentService {

    private SharedPreferences sharedPref;
    private ListPreferences listPreference;

    private Realm mRealm;

    private Retrofit apiRetrofit;
    private Retrofit libraryRetrofit;

    public LibraryDataService() {
        super("LibraryDataService");
    }

    public void onCreate() {
        Timber.d("LibraryDataService - onCreate");

        // Note there is a bug in GSON 2.5 that can cause it to StackOverflow when working with RealmObjects.
        // To work around this, use the ExclusionStrategy below or downgrade to 1.7.1
        // See more here: https://code.google.com/p/google-gson/issues/detail?id=440
        Type token = new TypeToken<RealmList<RealmStr>>() {
        }.getType();

        Gson gson = new GsonBuilder()
                .setDateFormat("MMMM dd, yyyy hh:mm:ss zzz")
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return f.getDeclaringClass().equals(RealmObject.class);
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .registerTypeAdapter(token, new TypeAdapter<RealmList<RealmStr>>() {

                    @Override
                    public void write(JsonWriter out, RealmList<RealmStr> value) throws io.realm.internal.IOException {
                        // Ignore
                    }

                    @Override
                    public RealmList<RealmStr> read(JsonReader in) throws io.realm.internal.IOException, java.io.IOException {
                        RealmList<RealmStr> list = new RealmList<RealmStr>();
                        in.beginArray();
                        while (in.hasNext()) {
                            list.add(new RealmStr(in.nextString()));
                        }
                        in.endArray();
                        return list;
                    }
                })
                .create();

        libraryRetrofit = new Retrofit.Builder()
                .baseUrl(Constants.LIBRARY_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        apiRetrofit = new Retrofit.Builder()
                .baseUrl(Constants.API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        super.onCreate();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        this.listPreference = ListPreferences.getInstance(getApplicationContext());
        this.sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Timber.d("LibraryDataService - Intent received!");
        
        // Create a new empty instance of Realm
        mRealm = Realm.getDefaultInstance();

        if (intent != null) {
            String action = intent.getAction();
            if(Constants.ACTION_GET_ALL_DATA.equals(action)){
                listPreference.setLastVehicleUpdate(System.currentTimeMillis());
                getAllAgency();
                getAllLocations();
                getAllAgencySwitches();
                getAllLocationSwitches();
                getAllPads();
                syncPadsToLocations();
                getAllMissions();
                getBaseVehicleDetails();
            } else if (Constants.ACTION_GET_AGENCY.equals(action)) {
                getAllAgency();
            } else if (Constants.ACTION_GET_MISSION.equals(action)){
                getAllMissions();
            } else if (Constants.ACTION_GET_LOCATION.equals(action)){
                getAllLocations();
            } else if (Constants.ACTION_GET_PADS.equals(action)){
                getAllPads();
            } else if (Constants.ACTION_GET_VEHICLES_DETAIL.equals(action)) {
                listPreference.setLastVehicleUpdate(System.currentTimeMillis());
                getBaseVehicleDetails();
                getLibraryRockets();
                getLibraryRocketsFamily();
            } else if (Constants.ACTION_GET_VEHICLES.equals(action)) {
                getLibraryRockets();
                getLibraryRocketsFamily();
            }
        }
        syncSwitchState(mRealm);
        mRealm.close();
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

        for (AgencySwitch locationSwitch : agencySwitches) {
            Agency agency = realm.where(Agency.class).equalTo("id", locationSwitch.getId()).findFirst();
            if (agency != null) {
                realm.beginTransaction();
                agency.setSubscribed(locationSwitch.isSubscribed());
                realm.copyToRealmOrUpdate(agency);
                realm.commitTransaction();
            }
        }
    }

    private void syncPadsToLocations() {
        mRealm.beginTransaction();
        RealmResults<Location> locationResults = mRealm.where(Location.class).findAll();
        RealmList<Location> locations = new RealmList<>();

        locations.addAll(locationResults.subList(0, locationResults.size()));
        for (Location location: locations){
            RealmList<Pad> pads = new RealmList<>();
            RealmResults<Pad> padsResults = mRealm.where(Pad.class).equalTo("locationid", location.getId()).findAll();
            pads.addAll(padsResults.subList(0, padsResults.size()));
            location.addPads(pads);
            mRealm.copyToRealmOrUpdate(location);
        }

        RealmResults<LocationSwitch> locationSwitchResults = mRealm.where(LocationSwitch.class).findAll();
        RealmList<LocationSwitch> locationSwitches = new RealmList<>();

        locationSwitches.addAll(locationSwitchResults.subList(0, locationSwitchResults.size()));
        for (LocationSwitch location : locationSwitches) {
            RealmList<Pad> pads = new RealmList<>();
            RealmResults<Pad> padsResults = mRealm.where(Pad.class).equalTo("locationid", location.getId()).findAll();
            pads.addAll(padsResults.subList(0, padsResults.size()));
            location.addPads(pads);
            mRealm.copyToRealmOrUpdate(location);
        }
        mRealm.commitTransaction();
    }

    private void getAllAgency() {
        LibraryRequestInterface request = libraryRetrofit.create(LibraryRequestInterface.class);
        Call<AgencyResponse> call;
        Response<AgencyResponse> launchResponse;
        RealmList<Agency> items = new RealmList<>();
        int offset = 0;
        int total = 10;
        int count;

        try {
            while (total != offset) {
                if (listPreference.isDebugEnabled()) {
                    call = request.getDebugAllAgency(offset);
                } else {
                    call = request.getAllAgency(offset);
                }
                launchResponse = call.execute();
                total = launchResponse.body().getTotal();
                count = launchResponse.body().getCount();
                offset = offset + count;
                Collections.addAll(items, launchResponse.body().getAgencies());
            }
            mRealm.beginTransaction();
            mRealm.copyToRealmOrUpdate(items);
            mRealm.commitTransaction();

            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(Constants.ACTION_SUCCESS_AGENCY);
            LibraryDataService.this.getApplicationContext().sendBroadcast(broadcastIntent);

        } catch (Exception e) {
            Crashlytics.logException(e);
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(Constants.ACTION_FAILURE_AGENCY);
            LibraryDataService.this.getApplicationContext().sendBroadcast(broadcastIntent);
        }
    }

    private void getAllAgencySwitches() {
        LibraryRequestInterface request = libraryRetrofit.create(LibraryRequestInterface.class);
        Call<AgencySwitchResponse> call;
        Response<AgencySwitchResponse> launchResponse;
        RealmList<AgencySwitch> items = new RealmList<>();
        int offset = 0;
        int total = 10;
        int count;

        try {
            while (total != offset) {
                if (listPreference.isDebugEnabled()) {
                    call = request.getDebugAllAgencySwitch(offset);
                } else {
                    call = request.getAllAgencySwitch(offset);
                }
                launchResponse = call.execute();
                total = launchResponse.body().getTotal();
                count = launchResponse.body().getCount();
                offset = offset + count;
                Collections.addAll(items, launchResponse.body().getAgencies());
            }
            mRealm.beginTransaction();
            for (AgencySwitch item : items) {
                AgencySwitch previous = mRealm.where(AgencySwitch.class)
                        .equalTo("id", item.getId())
                        .findFirst();
                if (previous != null) {
                    item.setSubscribed(previous.isSubscribed());
                }
                mRealm.copyToRealmOrUpdate(item);
            }
            mRealm.commitTransaction();

        } catch (Exception e) {
            Crashlytics.logException(e);
        }
    }

    private void getAllMissions() {
        LibraryRequestInterface request = libraryRetrofit.create(LibraryRequestInterface.class);
        Call<MissionResponse> call;
        Response<MissionResponse> launchResponse;
        RealmList<Mission> items = new RealmList<>();
        int offset = 0;
        int total = 10;
        int count;

        try {
            while (total != offset) {
                if (listPreference.isDebugEnabled()) {
                    call = request.getDebugAllMissions(offset);
                } else {
                    call = request.getAllMisisons(offset);
                }
                launchResponse = call.execute();
                total = launchResponse.body().getTotal();
                count = launchResponse.body().getCount();
                offset = offset + count;
                Collections.addAll(items, launchResponse.body().getMissions());
            }
            mRealm.beginTransaction();
            mRealm.copyToRealmOrUpdate(items);
            mRealm.commitTransaction();

            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(Constants.ACTION_SUCCESS_MISSIONS);
            LibraryDataService.this.getApplicationContext().sendBroadcast(broadcastIntent);

        } catch (Exception e) {
            Crashlytics.logException(e);
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(Constants.ACTION_FAILURE_MISSIONS);
            LibraryDataService.this.getApplicationContext().sendBroadcast(broadcastIntent);
        }
    }



    private void getAllLocations() {
        LibraryRequestInterface request = libraryRetrofit.create(LibraryRequestInterface.class);
        Call<LocationResponse> call;
        Response<LocationResponse> launchResponse;
        RealmList<Location> items = new RealmList<>();
        int offset = 0;
        int total = 10;
        int count;

        try {
            while (total != offset) {
                if (listPreference.isDebugEnabled()) {
                    call = request.getDebugLocations(offset);
                } else {
                    call = request.getLocations(offset);
                }
                launchResponse = call.execute();
                total = launchResponse.body().getTotal();
                count = launchResponse.body().getCount();
                offset = offset + count;
                Collections.addAll(items, launchResponse.body().getLocations());
            }
            mRealm.beginTransaction();
            for (Location item : items) {
                Location previous = mRealm.where(Location.class)
                        .equalTo("id", item.getId())
                        .findFirst();
                if (previous != null) {
                    item.setSubscribed(previous.isSubscribed());
                }
                mRealm.copyToRealmOrUpdate(item);
            }
            mRealm.commitTransaction();

            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(Constants.ACTION_SUCCESS_LOCATION);
            LibraryDataService.this.getApplicationContext().sendBroadcast(broadcastIntent);

        } catch (Exception e) {
            Crashlytics.logException(e);
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(Constants.ACTION_FAILURE_LOCATION);
            LibraryDataService.this.getApplicationContext().sendBroadcast(broadcastIntent);
        }
    }

    private void getAllLocationSwitches() {
        LibraryRequestInterface request = libraryRetrofit.create(LibraryRequestInterface.class);
        Call<LocationSwitchResponse> call;
        Response<LocationSwitchResponse> launchResponse;
        RealmList<LocationSwitch> items = new RealmList<>();
        int offset = 0;
        int total = 10;
        int count;

        try {
            while (total != offset) {
                if (listPreference.isDebugEnabled()) {
                    call = request.getDebugLocationsSwitches(offset);
                } else {
                    call = request.getLocationsSwitches(offset);
                }
                launchResponse = call.execute();
                total = launchResponse.body().getTotal();
                count = launchResponse.body().getCount();
                offset = offset + count;
                Collections.addAll(items, launchResponse.body().getLocations());
            }
            mRealm.beginTransaction();
            for (LocationSwitch item : items) {
                LocationSwitch previous = mRealm.where(LocationSwitch.class)
                        .equalTo("id", item.getId())
                        .findFirst();
                if (previous != null) {
                    item.setSubscribed(previous.isSubscribed());
                    mRealm.copyToRealmOrUpdate(item);
                } else {
                    mRealm.copyToRealmOrUpdate(item);
                }
            }
            mRealm.commitTransaction();

        } catch (Exception e) {
            Crashlytics.logException(e);
        }
    }

    private void getAllPads() {
        LibraryRequestInterface request = libraryRetrofit.create(LibraryRequestInterface.class);
        Call<PadResponse> call;
        Response<PadResponse> launchResponse;
        RealmList<Pad> items = new RealmList<>();
        int offset = 0;
        int total = 10;
        int count;

        try {
            while (total != offset) {
                if (listPreference.isDebugEnabled()) {
                    call = request.getDebugPads(offset);
                } else {
                    call = request.getPads(offset);
                }
                launchResponse = call.execute();
                total = launchResponse.body().getTotal();
                count = launchResponse.body().getCount();
                offset = offset + count;
                Collections.addAll(items, launchResponse.body().getPads());
            }
            mRealm.beginTransaction();
            mRealm.copyToRealmOrUpdate(items);
            mRealm.commitTransaction();

            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(Constants.ACTION_SUCCESS_PADS);
            LibraryDataService.this.getApplicationContext().sendBroadcast(broadcastIntent);

        } catch (Exception e) {
            Crashlytics.logException(e);
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(Constants.ACTION_FAILURE_PADS);
            LibraryDataService.this.getApplicationContext().sendBroadcast(broadcastIntent);
        }
    }

    private void getBaseVehicleDetails() {
        APIRequestInterface request = apiRetrofit.create(APIRequestInterface.class);
        Call<VehicleResponse> call;
        Response<VehicleResponse> launchResponse;
        RealmList<RocketDetails> items = new RealmList<>();

        try {
            call = request.getVehicles();
            launchResponse = call.execute();
            Collections.addAll(items, launchResponse.body().getVehicles());

            mRealm.beginTransaction();
            mRealm.copyToRealmOrUpdate(items);
            mRealm.commitTransaction();


            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(Constants.ACTION_SUCCESS_VEHICLE_DETAILS);
            LibraryDataService.this.getApplicationContext().sendBroadcast(broadcastIntent);
        } catch (IOException e) {
            Timber.e("VehicleDataService - ERROR: %s", e.getLocalizedMessage());
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(Constants.ACTION_FAILURE_VEHICLE_DETAILS);
            LibraryDataService.this.getApplicationContext().sendBroadcast(broadcastIntent);
        }
    }

    private void getLibraryRockets() {
        LibraryRequestInterface request = libraryRetrofit.create(LibraryRequestInterface.class);
        Call<RocketResponse> call;
        Response<RocketResponse> launchResponse;
        RealmList<Rocket> items = new RealmList<>();

        int offset = 0;
        int total = 10;
        int count;

        try {
            while (total != offset) {
                if (listPreference.isDebugEnabled()) {
                    call = request.getDebugAllRockets(offset);
                } else {
                    call = request.getAllRockets(offset);
                }
                launchResponse = call.execute();
                total = launchResponse.body().getTotal();
                count = launchResponse.body().getCount();
                offset = offset + count;
                Collections.addAll(items, launchResponse.body().getRockets());
            }

            mRealm.beginTransaction();
            mRealm.copyToRealmOrUpdate(items);
            mRealm.commitTransaction();

            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(Constants.ACTION_SUCCESS_VEHICLES);
            LibraryDataService.this.getApplicationContext().sendBroadcast(broadcastIntent);
        } catch (IOException e) {
            e.printStackTrace();
            Timber.e("VehicleDataService - ERROR: %s", e.getLocalizedMessage());

            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(Constants.ACTION_FAILURE_VEHICLES);
            LibraryDataService.this.getApplicationContext().sendBroadcast(broadcastIntent);
        }
    }

    //TODO does this need to send success/failure?
    private void getLibraryRocketsFamily() {
        LibraryRequestInterface request = libraryRetrofit.create(LibraryRequestInterface.class);
        Call<RocketFamilyResponse> call;
        Response<RocketFamilyResponse> launchResponse;
        RealmList<RocketFamily> items = new RealmList<>();

        int offset = 0;
        int total = 10;
        int count;

        try {
            while (total != offset) {
                if (listPreference.isDebugEnabled()) {
                    call = request.getDebugAllRocketFamily(offset);
                } else {
                    call = request.getAllRocketFamily(offset);
                }
                launchResponse = call.execute();
                total = launchResponse.body().getTotal();
                count = launchResponse.body().getCount();
                offset = offset + count;
                Collections.addAll(items, launchResponse.body().getRocketFamilies());
            }

            mRealm.beginTransaction();
            mRealm.copyToRealmOrUpdate(items);
            mRealm.commitTransaction();

        } catch (IOException e) {
            e.printStackTrace();
            Timber.e("VehicleDataService - ERROR: %s", e.getLocalizedMessage());
        }
    }
}
