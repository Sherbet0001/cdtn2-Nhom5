package com.nhom5.partjob.repository;

import com.nhom5.partjob.domain.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findAllByOrderByCityAscDistrictAsc();
}
