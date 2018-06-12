package me.calebjones.spacelaunchnow.ui.launchdetail.activity;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.appbar.AppBarLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.youtube.player.YouTubePlayer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;
import io.realm.Realm;
import io.realm.RealmList;
import me.calebjones.spacelaunchnow.R;
import me.calebjones.spacelaunchnow.common.BaseActivity;
import me.calebjones.spacelaunchnow.common.customviews.generate.OnFeedbackListener;
import me.calebjones.spacelaunchnow.common.customviews.generate.Rate;
import me.calebjones.spacelaunchnow.content.database.ListPreferences;
import me.calebjones.spacelaunchnow.content.events.LaunchEvent;
import me.calebjones.spacelaunchnow.content.events.LaunchRequestEvent;
import me.calebjones.spacelaunchnow.data.models.launchlibrary.Launch;
import me.calebjones.spacelaunchnow.data.models.spacelaunchnow.RocketDetail;
import me.calebjones.spacelaunchnow.data.networking.DataClient;
import me.calebjones.spacelaunchnow.data.networking.error.ErrorUtil;
import me.calebjones.spacelaunchnow.data.networking.error.LibraryError;
import me.calebjones.spacelaunchnow.data.networking.responses.launchlibrary.LaunchResponse;
import me.calebjones.spacelaunchnow.ui.imageviewer.FullscreenImageActivity;
import me.calebjones.spacelaunchnow.ui.launchdetail.TabsAdapter;
import me.calebjones.spacelaunchnow.ui.main.MainActivity;
import me.calebjones.spacelaunchnow.ui.supporter.SupporterHelper;
import me.calebjones.spacelaunchnow.utils.GlideApp;
import me.calebjones.spacelaunchnow.utils.Utils;
import me.calebjones.spacelaunchnow.utils.analytics.Analytics;
import me.calebjones.spacelaunchnow.utils.customtab.CustomTabActivityHelper;
import me.calebjones.spacelaunchnow.utils.views.CustomOnOffsetChangedListener;
import me.calebjones.spacelaunchnow.utils.views.SnackbarHandler;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class LaunchDetailActivity extends BaseActivity
        implements AppBarLayout.OnOffsetChangedListener {

    private static final int PERCENTAGE_TO_ANIMATE_AVATAR = 20;
    @BindView(R.id.fab_share)
    FloatingActionButton fabShare;
    @BindView(R.id.adView)
    AdView adView;
    @BindView(R.id.detail_profile_image)
    CircleImageView detail_profile_image;
    @BindView(R.id.detail_title)
    TextView detail_rocket;
    @BindView(R.id.detail_mission_location)
    TextView detail_mission_location;
    @BindView(R.id.detail_viewpager)
    ViewPager viewPager;
    @BindView(R.id.rootview)
    CoordinatorLayout rootview;
    @BindView(R.id.detail_swipe_refresh)
    SwipeRefreshLayout detailSwipeRefresh;
    @BindView(R.id.detail_tabs)
    TabLayout tabLayout;
    @BindView(R.id.detail_profile_backdrop)
    ImageView detail_profile_backdrop;
    @BindView(R.id.detail_appbar)
    AppBarLayout appBarLayout;
    @BindView(R.id.detail_toolbar)
    Toolbar toolbar;

    private boolean mIsAvatarShown = true;

    private int mMaxScrollSize;
    private SharedPreferences sharedPref;
    private ListPreferences sharedPreference;
    private CustomTabActivityHelper customTabActivityHelper;
    private Context context;
    private TabsAdapter tabAdapter;
    private int statusColor;
    public YouTubePlayer youTubePlayer;
    public boolean isYouTubePlayerFullScreen;
    public String response;
    public Launch launch;
    private boolean fabShowable = true;
    private Realm realm;
    private Rate rate;

    public LaunchDetailActivity() {
        super("Launch Detail Activity");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int m_theme;

        realm = getRealm();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        context = getApplicationContext();
        customTabActivityHelper = new CustomTabActivityHelper();
        sharedPreference = ListPreferences.getInstance(context);

        if (sharedPreference.isNightModeActive(this)) {
            statusColor = ContextCompat.getColor(context, R.color.darkPrimary_dark);
        } else {
            statusColor = ContextCompat.getColor(context, R.color.colorPrimaryDark);
        }
        m_theme = R.style.BaseAppTheme;

        if (getSharedPreferences("theme_changed", 0).getBoolean("recreate", false)) {
            SharedPreferences.Editor editor = getSharedPreferences("theme_changed", 0).edit();
            editor.putBoolean("recreate", false);
            editor.apply();
            recreate();
        }


        setTheme(m_theme);
        setContentView(R.layout.activity_launch_detail);
        ButterKnife.bind(this);
        detailSwipeRefresh.setEnabled(false);

        rate = new Rate.Builder(context)
                .setTriggerCount(10)
                .setMinimumInstallTime(TimeUnit.DAYS.toMillis(3))
                .setMessage(R.string.please_rate_short)
                .setFeedbackAction(new OnFeedbackListener() {
                    @Override
                    public void onFeedbackTapped() {
                        showFeedback();
                    }
                })
                .setSnackBarParent(rootview)
                .build();

        rate.showRequest();

        if (!SupporterHelper.isSupporter()) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
            adView.setAdListener(new AdListener() {

                @Override
                public void onAdLoaded() {
                    adView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAdFailedToLoad(int error) {
                    adView.setVisibility(View.GONE);
                }

            });
        } else {
            adView.setVisibility(View.GONE);
        }

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        //Grab information from Intent
        Intent mIntent = getIntent();
        String type = mIntent.getStringExtra("TYPE");

        if (type != null && type.equals("launch")) {
            final int id = mIntent.getIntExtra("launchID", 0);

            Launch launch = getRealm().where(Launch.class).equalTo("id", id).findFirst();
            if (launch != null) {
                updateViews(launch);
            }
            if (savedInstanceState == null) {
                detailSwipeRefresh.setRefreshing(true);
                DataClient.getInstance().getLaunchById(id, true, new Callback<LaunchResponse>() {
                    @Override
                    public void onResponse(Call<LaunchResponse> call, Response<LaunchResponse> response) {
                        if (response.isSuccessful()) {
                            final RealmList<Launch> items = new RealmList<>(response.body().getLaunches());
                            if (items.size() == 1) {
                                final Launch item = items.first();
                                getRealm().executeTransactionAsync(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm bgRealm) {
                                        Launch previous = bgRealm.where(Launch.class)
                                                .equalTo("id", item.getId())
                                                .findFirst();
                                        if (previous != null) {
                                            item.setEventID(previous.getEventID());
                                            item.setSyncCalendar(previous.syncCalendar());
                                            item.setLaunchTimeStamp(previous.getLaunchTimeStamp());
                                            item.setIsNotifiedDay(previous.getIsNotifiedDay());
                                            item.setIsNotifiedHour(previous.getIsNotifiedHour());
                                            item.setIsNotifiedTenMinute(previous.getIsNotifiedTenMinute());
                                            item.setNotifiable(previous.isNotifiable());
                                        }
                                        item.getLocation().setPrimaryID();
                                        bgRealm.copyToRealmOrUpdate(item);
                                        Timber.v("Updated detailLaunch: %s", item.getId());
                                    }

                                }, new Realm.Transaction.OnSuccess() {
                                    @Override
                                    public void onSuccess() {
                                        sendUpdateView(id);
                                    }
                                });
                            }
                        } else {
                            LibraryError error = ErrorUtil.parseLibraryError(response);
                            if (error.getMessage() != null && error.getMessage().contains("None found")) {
                                final Launch launch = getRealm().where(Launch.class).equalTo("id", id).findFirst();
                                if (launch != null) {
                                    getRealm().executeTransaction(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            launch.deleteFromRealm();
                                        }
                                    });
                                }
                                Toast.makeText(LaunchDetailActivity.this, R.string.error_loading_launch, Toast.LENGTH_SHORT).show();
                                onBackPressed();
                            }
                        }

                        detailSwipeRefresh.setRefreshing(false);
                    }

                    @Override
                    public void onFailure(Call<LaunchResponse> call, Throwable t) {
                        Launch item = getRealm().where(Launch.class)
                                .equalTo("id", id)
                                .findFirst();
                        if (item != null) {
                            updateViews(item);
                        }
                        detailSwipeRefresh.setRefreshing(false);
                    }
                });
            }
        }

        toolbar.setNavigationOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, MainActivity.class);
                        startActivity(intent);

                    }
                }

        );
        appBarLayout.addOnOffsetChangedListener(new CustomOnOffsetChangedListener(statusColor, getWindow()));
        appBarLayout.addOnOffsetChangedListener(this);
        mMaxScrollSize = appBarLayout.getTotalScrollRange();

        tabAdapter = new TabsAdapter(getSupportFragmentManager(), context);

        viewPager.setAdapter(tabAdapter);
        viewPager.setOffscreenPageLimit(3);

        tabLayout.setupWithViewPager(viewPager);
    }

    private void showFeedback() {
        new MaterialDialog.Builder(this)
                .title(R.string.feedback_title)
                .autoDismiss(true)
                .content(R.string.feedback_description)
                .neutralColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .negativeText(R.string.launch_data)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        String url = getString(R.string.launch_library_reddit);
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        startActivity(i);
                    }
                })
                .positiveColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .positiveText(R.string.app_feedback)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.getBuilder()
                                .title(R.string.need_support)
                                .content(R.string.need_support_description)
                                .neutralText(R.string.email)
                                .negativeText(R.string.cancel)
                                .positiveText(R.string.discord)
                                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(MaterialDialog dialog, DialogAction which) {
                                        Intent intent = new Intent(Intent.ACTION_SENDTO);
                                        intent.setData(Uri.parse("mailto:"));
                                        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@spacelaunchnow.me"});
                                        intent.putExtra(Intent.EXTRA_SUBJECT, "Space Launch Now - Feedback");

                                        startActivity(Intent.createChooser(intent, "Email via..."));
                                    }
                                })
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(MaterialDialog dialog, DialogAction which) {
                                        String url = "https://discord.gg/WVfzEDW";
                                        Intent i = new Intent(Intent.ACTION_VIEW);
                                        i.setData(Uri.parse(url));
                                        startActivity(i);
                                    }
                                })
                                .onNegative(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        dialog.dismiss();
                                    }
                                })
                                .show();
                    }
                })
                .show();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }
    }


    private  void sendUpdateView(int id){
        Launch item = getRealm().where(Launch.class)
                .equalTo("id", id)
                .findFirst();
        if (item != null) {
            updateViews(item);
        } else {
            fabShare.hide();
        }
    }

    private void updateViews(Launch launch) {
        try {
            this.launch = launch;

            EventBus.getDefault().post(new LaunchEvent(launch));
            if (!this.isDestroyed() && launch != null && launch.getRocket() != null) {
                Timber.v("Loading detailLaunch %s", launch.getId());
                findProfileLogo();
                if (launch.getRocket().getName() != null) {
                    if (launch.getRocket().getImageURL() != null
                            && launch.getRocket().getImageURL().length() > 0
                            && !launch.getRocket().getImageURL().contains("placeholder")) {
                        final String image =  launch.getRocket().getImageURL();
                        GlideApp.with(this)
                                .load(image)
                                .placeholder(R.drawable.placeholder)
                                .into(detail_profile_backdrop);
                        detail_profile_backdrop.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                showImageFullscreen(image);
                            }
                        });
                        getLaunchVehicle(launch, false);
                    } else {
                        getLaunchVehicle(launch, true);
                    }
                }
            } else if (this.isDestroyed()) {
                Timber.v("DetailLaunch is destroyed, stopping loading data.");
            }

            //Assign the title and mission location data
            detail_rocket.setText(launch.getName());
            fabShare.show();
        } catch (NoSuchMethodError e) {
            Timber.e(e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void findProfileLogo() {

        //Default location, mission is unknown.
        String location = getString(R.string.unknown_location);
        String mission = getString(R.string.unknown_mission);
        String locationCountryCode = null;
        String agencyName = null;
        //This checks to see if a location is available

        if (launch.getLsp() != null) {
            locationCountryCode = launch.getLsp().getCountryCode();
            agencyName = launch.getLsp().getName();
            //Go through various CountryCodes and assign flag.
            if (launch.getLsp().getAbbrev().contains("ASA")) {
                applyProfileLogo(getString(R.string.ariane_logo));
            } else if (launch.getLsp().getAbbrev().contains("SpX")) {
                applyProfileLogo(getString(R.string.spacex_logo));
            } else if (launch.getLsp().getAbbrev().contains("BA")) {
                applyProfileLogo(getString(R.string.Yuzhnoye_logo));
            } else if (launch.getLsp().getAbbrev().contains("ULA")) {
                applyProfileLogo(getString(R.string.ula_logo));
            } else if (locationCountryCode.length() == 3) {
                if (locationCountryCode.contains("USA")) {
                    applyProfileLogo(getString(R.string.usa_flag));
                } else if (locationCountryCode.contains("RUS")) {
                    applyProfileLogo(getString(R.string.rus_logo));
                } else if (locationCountryCode.contains("CHN")) {
                    applyProfileLogo(getString(R.string.chn_logo));
                } else if (locationCountryCode.contains("IND")) {
                    applyProfileLogo(getString(R.string.ind_logo));
                } else if (locationCountryCode.contains("JPN")) {
                    applyProfileLogo(getString(R.string.jpn_logo));
                }
            }
        } else if (launch.getLocation() != null && launch.getLocation().getPads() != null
                && launch.getLocation().getPads().size() > 0
                && launch.getLocation().getPads().get(0).getAgencies() != null
                && launch.getLocation().getPads().get(0).getAgencies().size() > 0) {
            locationCountryCode = launch.getLocation().getPads().
                    get(0).getAgencies().get(0).getCountryCode();
            agencyName = launch.getLocation().getPads().
                    get(0).getAgencies().get(0).getName();
            if (locationCountryCode.length() == 3) {
                if (locationCountryCode.contains("USA")) {
                    applyProfileLogo(getString(R.string.usa_flag));
                } else if (locationCountryCode.contains("RUS")) {
                    applyProfileLogo(getString(R.string.rus_logo));
                } else if (locationCountryCode.contains("CHN")) {
                    applyProfileLogo(getString(R.string.chn_logo));
                } else if (locationCountryCode.contains("IND")) {
                    applyProfileLogo(getString(R.string.ind_logo));
                } else if (locationCountryCode.contains("JPN")) {
                    applyProfileLogo(getString(R.string.jpn_logo));
                }
            }
        }

        Timber.v("LaunchDetailActivity - CountryCode: %s - LSP: %s - %s",
                String.valueOf(locationCountryCode), locationCountryCode, agencyName);

        if (launch.getLocation() != null && launch.getLocation().getPads() != null && launch.getLocation().getPads().size() > 0) {
            location = (launch.getLocation().getPads().get(0).getName());
        }
        //Assigns the result of the two above checks.
        detail_mission_location.setText(location);
    }

    private void applyProfileLogo(String url) {
        Timber.d("LaunchDetailActivity - Loading Profile Image url: %s ", url);

        GlideApp.with(this)
                .load(url)
                .placeholder(R.drawable.icon_international)
                .error(R.drawable.icon_international)
                .into(detail_profile_image);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void getLaunchVehicle(Launch result, boolean setImage) {
        String query;
        if (result.getRocket().getName().contains("Space Shuttle")) {
            query = "Space Shuttle";
        } else {
            query = result.getRocket().getName();
        }
        RocketDetail launchVehicle = getRealm().where(RocketDetail.class)
                .contains("name", query)
                .findFirst();
        if (setImage) {
            if (launchVehicle != null && launchVehicle.getImageURL()  != null && launchVehicle.getImageURL().length() > 0 && !launchVehicle.getImageURL().contains("placeholder")) {
                GlideApp.with(this)
                        .load(launchVehicle.getImageURL())
                        .into(detail_profile_backdrop);
                Timber.d("Glide Loading: %s %s", launchVehicle.getName(), launchVehicle.getImageURL());
            }
        }
    }

    public void setData(String data) {
        response = data;
        Timber.v("LaunchDetailActivity - %s", response);
        Scanner scanner = new Scanner(response);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            // process the line
            Timber.v("setData - %s ", line);
        }
        scanner.close();
    }

    public Launch getLaunch() {
        return launch;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {

        if (mMaxScrollSize == 0) {
            mMaxScrollSize = appBarLayout.getTotalScrollRange();
        }

        int percentage = (Math.abs(verticalOffset)) * 100 / mMaxScrollSize;

        if (percentage >= PERCENTAGE_TO_ANIMATE_AVATAR && mIsAvatarShown) {
            mIsAvatarShown = false;
            detail_profile_image.animate()
                    .scaleY(0).scaleX(0)
                    .setDuration(200)
                    .start();
            fabShare.hide();
        }

        if (percentage <= PERCENTAGE_TO_ANIMATE_AVATAR && !mIsAvatarShown) {
            mIsAvatarShown = true;

            detail_profile_image.animate()
                    .scaleY(1).scaleX(1)
                    .start();

            if(fabShowable) {
                fabShare.show();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        Timber.v("LaunchDetailActivity onStart!");
        customTabActivityHelper.bindCustomTabsService(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        Timber.v("LaunchDetailActivity onStop!");
        customTabActivityHelper.unbindCustomTabsService(this);
        super.onStop();
    }

    public void mayLaunchUrl(Uri parse) {
        if (customTabActivityHelper.mayLaunchUrl(parse, null, null)) {
            Timber.v("mayLaunchURL Accepted - %s", parse.toString());
        } else {
            Timber.v("mayLaunchURL Denied - %s", parse.toString());
        }
    }

    @OnClick(R.id.fab_share)
    public void onViewClicked() {
        String launchDate = "";
        String message = "";
        try {
            if (launch.getNet() != null) {
                Date date = launch.getNet();
                SimpleDateFormat df = Utils.getSimpleDateFormatForUI("EEEE, MMMM dd, yyyy - hh:mm a zzz");
                df.toLocalizedPattern();
                launchDate = df.format(date);
            }
            if (launch.getLocation() != null && launch.getLocation().getPads() != null && launch.getLocation().getPads().size() > 0 && launch.getLocation().getPads().get(0).getAgencies() != null && launch.getLocation().getPads().
                    get(0).getAgencies().size() > 0) {

                message = launch.getName() + " launching from "
                        + launch.getLocation().getName() + "\n\n"
                        + launchDate;
            } else if (launch.getLocation() != null) {
                message = launch.getName() + " launching from "
                        + launch.getLocation().getName() + "\n\n"
                        + launchDate;
            } else {
                message = launch.getName()
                        + "\n\n"
                        + launchDate;
            }
        } catch (NullPointerException e) {
            Timber.e(e);
        }
        if (launch.getName() != null && launch.getUrl() != null) {
            ShareCompat.IntentBuilder.from(this)
                    .setType("text/plain")
                    .setChooserTitle("Share: " + launch.getName())
                    .setText(String.format("%s\n\nWatch Live: %s", message, launch.getUrl()))
                    .startChooser();
            Analytics.getInstance().sendLaunchShared("Share FAB", launch.getName() + "-" + launch.getId().toString());
        } else {
            SnackbarHandler.showErrorSnackbar(this, rootview, "Error - unable to share this launch.");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(LaunchRequestEvent event) {
        EventBus.getDefault().post(new LaunchEvent(launch));
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        if (youTubePlayer != null && isYouTubePlayerFullScreen){
            youTubePlayer.setFullscreen(false);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onLowMemory(){
        Timber.v("onLowMemory");
//        adView.destroy();
        super.onLowMemory();
    }

    public void videoPlaying() {
        fabShowable = false;
        fabShare.hide();
    }

    public void videoStopped() {
        fabShowable = true;
        fabShare.show();
    }


    public void showImageFullscreen(String backdropImage){
        if (backdropImage != null) {
            Intent animateIntent = new Intent(this, FullscreenImageActivity.class);
            animateIntent.putExtra("imageURL", backdropImage);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                this.startActivity(animateIntent, ActivityOptions.makeSceneTransitionAnimation(this, detail_profile_backdrop, "imageCover").toBundle());
            } else {
                this.startActivity(animateIntent);
            }
        }
    }
}
