
package me.calebjones.spacelaunchnow.data.models.realm;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Pad extends RealmObject {

    @PrimaryKey
    private Integer id;
    private Integer padType;
    private Integer locationid;
    private Integer retired;

    private String name;
    private String infoURL;
    private String wikiURL;
    private String mapURL;
    private Double latitude;
    private Double longitude;
    private RealmList<Agency> agencies = new RealmList<>();
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

    public String getInfoURL() {
        return infoURL;
    }

    public void setInfoURL(String infoURL) {
        this.infoURL = infoURL;
    }

    public String getWikiURL() {
        return wikiURL;
    }

    public void setWikiURL(String wikiURL) {
        this.wikiURL = wikiURL;
    }

    public String getMapURL() {
        return mapURL;
    }

    public void setMapURL(String mapURL) {
        this.mapURL = mapURL;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public RealmList<Agency> getAgencies() {
        return agencies;
    }

    public void setAgencies(RealmList<Agency> agencies) {
        this.agencies = agencies;
    }

    public Integer getPadType() {
        return padType;
    }

    public void setPadType(Integer padType) {
        this.padType = padType;
    }

    public Integer getRetired() {
        return retired;
    }

    public void setRetired(Integer retired) {
        this.retired = retired;
    }

    public Integer getLocationid() {
        return locationid;
    }

    public void setLocationid(Integer locationid) {
        this.locationid = locationid;
    }
}
