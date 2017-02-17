package eu.h2020.symbiote.model;

import org.springframework.data.annotation.Id;

/**
 * Registry Location object used with Resources.
 *
 * Created by mateuszl
 */
public class Location {
    @Id
    private String id;
    private String name;
    private String description;
    private double latitude;
    private double longitude;
    private double altitude;

    public Location() {

    }

    /**
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * @param latitude
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * @return
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * @param longitude
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /**
     * @return
     */
    public double getAltitude() {
        return altitude;
    }

    /**
     * @param altitude
     */
    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }
}
