package me.calebjones.spacelaunchnow.data.networking.responses.launchlibrary;

import me.calebjones.spacelaunchnow.data.models.realm.AgencySwitch;

public class AgencySwitchResponse extends BaseResponse {
    private AgencySwitch[] agencies;

    public AgencySwitch[] getAgencies() {
        return agencies;
    }
}
