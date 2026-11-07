package com.marketplace.repositories;

import com.marketplace.entities.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findAllByOrderByIdAsc();

    boolean existsByCityIgnoreCaseAndDistrictIgnoreCase(String city, String district);

    boolean existsByCityIgnoreCaseAndDistrictIgnoreCaseAndIdNot(String city, String district, Long id);
}

