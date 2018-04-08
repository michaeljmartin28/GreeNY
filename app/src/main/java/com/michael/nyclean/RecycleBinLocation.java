package com.michael.nyclean;

import android.support.annotation.NonNull;

public class RecycleBinLocation implements Comparable<RecycleBinLocation>{
    /*
        // Description of one JSON object for a Recycle Bin


       {
        "address": "E 227 St/Bronx River Pkway",
        "borough": "Bronx",
        "latitude": "40.890848989",
        "longitude": "-73.864223918",
        "park_site_name": "227th St. Plgd",
        "site_type": "Subproperty"
    }
     */
    private String address;
    private String borough;
    private String parkSiteName;
    private String siteType;
    private double distance;
    private double latitude;
    private double longitude;

    public RecycleBinLocation(String address, String borough, double latitude, double longitude, String parkSiteName, String siteType) {
        this.address = address;
        this.borough = borough;
        this.latitude = latitude;
        this.longitude = longitude;
        this.parkSiteName = parkSiteName;
        this.siteType = siteType;
        this.distance = 0;
    }


    @Override
    public int compareTo(@NonNull RecycleBinLocation recycleBin){
        double compareDistance = recycleBin.getDistance();

        if (distance < compareDistance)
            return -1;
        else if (distance > compareDistance)
            return 1;
        else
            return 0;
    }

    // getters and setters
    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBorough() {
        return borough;
    }

    public void setBorough(String borough) {
        this.borough = borough;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getParkSiteName() {
        return parkSiteName;
    }

    public void setParkSiteName(String parkSiteName) {
        this.parkSiteName = parkSiteName;
    }

    public String getSiteType() {
        return siteType;
    }

    public void setSiteType(String siteType) {
        this.siteType = siteType;
    }

    @Override
    public String toString() {
        return "RecycleBinLocation{" +
                "address='" + address + '\'' +
                ", borough='" + borough + '\'' +
                ", parkSiteName='" + parkSiteName + '\'' +
                ", siteType='" + siteType + '\'' +
                ", distance=" + distance +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
