package com.nhom5.partjob.domain;

import jakarta.persistence.*;

/** Dia diem (tbl_locations). UQ(city, district). */
@Entity
@Table(name = "tbl_locations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"city", "district"}))
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city", nullable = false, length = 80)
    private String city;

    @Column(name = "district", nullable = false, length = 80)
    private String district;

    protected Location() {
    }

    public Location(String city, String district) {
        this.city = city;
        this.district = district;
    }

    public Long getId() { return id; }
    public String getCity() { return city; }
    public String getDistrict() { return district; }
}
