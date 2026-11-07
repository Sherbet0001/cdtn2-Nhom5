package com.vlu.freelance.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tbl_locations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"city", "district"})
})
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, length = 80)
    private String district;

    public Location() {}

    public Location(Long id, String city, String district) {
        this.id = id;
        this.city = city;
        this.district = district;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getFormattedLocation() {
        if (district != null && city != null) {
            return district + ", " + city;
        }
        return city != null ? city : "";
    }
}
