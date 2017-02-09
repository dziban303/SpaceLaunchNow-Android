package me.calebjones.spacelaunchnow.subscriptions;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatCheckBox;

import io.realm.Realm;
import me.calebjones.spacelaunchnow.common.BaseNavigator;
import me.calebjones.spacelaunchnow.common.BasePresenter;
import me.calebjones.spacelaunchnow.common.BaseView;
import me.calebjones.spacelaunchnow.data.models.realm.AgencySwitch;
import me.calebjones.spacelaunchnow.data.models.realm.LocationSwitch;

public interface SubscriptionContract {

    interface Navigator extends BaseNavigator {

        void goHome();

    }

    interface NavigatorProvider {

        @NonNull
        Navigator getNavigator(SubscriptionContract.Presenter presenter);
    }

    interface View extends BaseView<Presenter> {

        void showSnackbarMessage(String message);

        void updateAllSwitches(boolean state);

        void updateSwitches();

        void addViewToCustomList(AppCompatCheckBox appCompatCheckBox);

        void addViewToLocationView(AppCompatCheckBox appCompatCheckBox);

        void addViewToAgencyView(AppCompatCheckBox appCompatCheckBox);

        void updateCheckboxByID(int id, boolean state);

        void updateView(AppCompatCheckBox appCompatCheckBox, boolean state);

        AppCompatCheckBox createCheckbox(AgencySwitch agencySwitch);

        AppCompatCheckBox createCheckbox(LocationSwitch locationSwitch);

    }

    interface Presenter extends BasePresenter {

        void onHomeClicked();

        void setNavigator(@NonNull Navigator navigator);

        void checkAllClicked(Realm realm,boolean value);

        void initializeSwitches();

        void initializeDynamicSwitches(Context context);

        void checkboxSelected(AppCompatCheckBox compatCheckBox);
    }
}
