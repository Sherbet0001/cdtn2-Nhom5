package com.marketplace.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "tbl_locations",
    uniqueConstraints = @UniqueConstraint(name = "uk_city_district", columnNames = {"city", "district"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city", nullable = false, length = 80)
    private String city;

    @Column(name = "district", nullable = false, length = 80)
    private String district;
}
