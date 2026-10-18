package com.scanin.dao.repository;

import com.scanin.dao.entity.OfficeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfficeConfigRepository extends JpaRepository<OfficeConfig, Long> {

}