
package me.calebjones.spacelaunchnow.data.models.realm;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class LocationSwitch extends RealmObject {

    @PrimaryKey
    private Integer id;
    private String name;
    private RealmList<Pad> pads = new RealmList<>();
    private boolean isSubscribed = true;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RealmList<Pad> getPads() {
        return pads;
    }

    public void setPads(RealmList<Pad> pads) {
        this.pads = pads;
    }

    public void addPads(RealmList<Pad> pads) {
        this.pads.addAll(pads);
    }

    public void addPad(Pad pad) {
        this.pads.add(pad);
    }

    public boolean isSubscribed() {
        return isSubscribed;
    }

    public void setSubscribed(boolean subscribed) {
        isSubscribed = subscribed;
    }
}
