package com.scanin.dao.repository;

import com.scanin.dao.entity.Attendance;
import com.scanin.dao.entity.User;
import com.scanin.util.ArrivalStatus;
import com.scanin.util.DepartureStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByUserAndWorkDate(User user, LocalDate workDate);

    boolean existsByWorkDate(LocalDate workDate);

    long countByWorkDateAndDepartureStatus(LocalDate workDate, DepartureStatus departureStatus);

    List<Attendance> findAllByWorkDateAndDepartureStatus(LocalDate workDate, DepartureStatus departureStatus);

    List<Attendance> findAllByWorkDateOrderByCheckInTimeAsc(LocalDate workDate);

    long countByUserAndWorkDateBetweenAndArrivalStatus(
            User user, LocalDate startDate, LocalDate endDate, ArrivalStatus arrivalStatus
    );
}