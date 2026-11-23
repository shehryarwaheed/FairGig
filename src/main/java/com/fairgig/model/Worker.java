package com.fairgig.model;

/** A gig worker (tables "users" + "workers"). */
public class Worker extends User {

    private String phone;       // optional
    private String city;
    private String cityZone;

    public Worker() {
        super(Role.WORKER);
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCityZone() { return cityZone; }
    public void setCityZone(String cityZone) { this.cityZone = cityZone; }
}
