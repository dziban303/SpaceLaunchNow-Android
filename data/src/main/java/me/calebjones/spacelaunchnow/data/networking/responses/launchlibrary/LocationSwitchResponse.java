package me.calebjones.spacelaunchnow.data.networking.responses.launchlibrary;

import me.calebjones.spacelaunchnow.data.models.realm.LocationSwitch;

public class LocationSwitchResponse extends BaseResponse{
    private LocationSwitch[] locations;

    public LocationSwitch[] getLocations() {
        return locations;
    }
}
