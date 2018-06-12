package me.calebjones.spacelaunchnow.ui.main.vehicles.launcher;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.calebjones.spacelaunchnow.R;
import me.calebjones.spacelaunchnow.common.RetroFitFragment;
import me.calebjones.spacelaunchnow.data.models.spacelaunchnow.SLNAgency;
import me.calebjones.spacelaunchnow.data.networking.error.ErrorUtil;
import me.calebjones.spacelaunchnow.data.networking.interfaces.SpaceLaunchNowService;
import me.calebjones.spacelaunchnow.data.networking.responses.base.LauncherResponse;
import me.calebjones.spacelaunchnow.ui.launcher.LauncherDetailActivity;
import me.calebjones.spacelaunchnow.utils.analytics.Analytics;
import me.calebjones.spacelaunchnow.utils.OnItemClickListener;
import me.calebjones.spacelaunchnow.utils.views.SnackbarHandler;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class LauncherFragment extends RetroFitFragment implements SwipeRefreshLayout.OnRefreshListener {

    private VehicleAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private List<SLNAgency> items = new ArrayList<>();
    private Context context;
    private View view;
    private RecyclerView mRecyclerView;
    private CoordinatorLayout coordinatorLayout;
    private SwipeRefreshLayout swipeRefreshLayout;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        adapter = new VehicleAdapter(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        view = inflater.inflate(R.layout.fragment_launch_vehicles, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.vehicle_detail_list);
        coordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.vehicle_coordinator);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(this);

        if (getResources().getBoolean(R.bool.landscape) && getResources().getBoolean(R.bool.isTablet)) {
            layoutManager = new GridLayoutManager(context, 3);
        } else if (getResources().getBoolean(R.bool.landscape)  || getResources().getBoolean(R.bool.isTablet)) {
            layoutManager = new GridLayoutManager(context, 2);
        } else {
            layoutManager = new LinearLayoutManager(context);
        }

        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int topRowVerticalPosition =
                        (recyclerView == null || recyclerView.getChildCount() == 0) ? 0 : recyclerView.getChildAt(0).getTop();
                swipeRefreshLayout.setEnabled(topRowVerticalPosition >= 0);

            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
        adapter.setOnItemClickListener(recyclerRowClickListener);
        mRecyclerView.setAdapter(adapter);
        Timber.v("Returning view.");
        return view;
    }



    @Override
    public void onResume() {
        super.onResume();
        Timber.v("onResume");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loadJSON();
            }
        }, 100);
    }

    private void loadJSON() {
        Timber.v("Loading vehicles...");
        showLoading();

        SpaceLaunchNowService request = getSpaceLaunchNowRetrofit().create(SpaceLaunchNowService.class);
        Call<LauncherResponse> call = request.getVehicleAgencies(true);

        call.enqueue(new Callback<LauncherResponse>() {
            @Override
            public void onResponse(Call<LauncherResponse> call, Response<LauncherResponse> response) {
                Timber.v("onResponse");
                if (response.raw().cacheResponse() != null) {
                    Timber.v("Response pulled from cache.");
                }

                if (response.raw().networkResponse() != null) {
                    Timber.v("Response pulled from network.");
                }

                if (response.isSuccessful()) {
                    LauncherResponse jsonResponse = response.body();
                    Timber.v("Success %s", response.message());
                    items = new ArrayList<>(Arrays.asList(jsonResponse.getLaunchers()));
                    adapter.addItems(items);
                    Analytics.getInstance().sendNetworkEvent("LAUNCHER_INFORMATION", call.request().url().toString(), true);

                } else {
                    Timber.e(ErrorUtil.parseSpaceLaunchNowError(response).message());
                    SnackbarHandler.showErrorSnackbar(context, coordinatorLayout, ErrorUtil.parseSpaceLaunchNowError(response).message());
                }
                hideLoading();
            }

            @Override
            public void onFailure(Call<LauncherResponse> call, Throwable t) {
                Timber.e(t.getMessage());
                hideLoading();
                SnackbarHandler.showErrorSnackbar(context, coordinatorLayout, t.getLocalizedMessage());
                Analytics.getInstance().sendNetworkEvent("VEHICLE_INFORMATION", call.request().url().toString(), false, t.getLocalizedMessage());
            }
        });
    }

    private void hideLoading() {
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void showLoading() {
        if (!swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(true);
        }
    }

    private OnItemClickListener recyclerRowClickListener = new OnItemClickListener() {

        @Override
        public void onClick(View v, int position) {
            Analytics.getInstance().sendButtonClicked("Launcher clicked", items.get(position).getName());
            Gson gson = new Gson();
            String jsonItem = gson.toJson(items.get(position));

            Intent intent = new Intent(getActivity(), LauncherDetailActivity.class);
            intent.putExtra("name", items.get(position).getName());
            intent.putExtra("json", jsonItem);
            startActivity(intent);
        }

    };

    @Override
    public void onRefresh() {
        Analytics.getInstance().sendButtonClicked("Launcher Refresh");
        loadJSON();
    }
}
