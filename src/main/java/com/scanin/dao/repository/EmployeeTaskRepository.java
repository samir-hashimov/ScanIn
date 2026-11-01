package com.scanin.dao.repository;

import com.scanin.dao.entity.EmployeeTask;
import com.scanin.dao.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeTaskRepository extends JpaRepository<EmployeeTask, Long> {


    List<EmployeeTask> findAllByUserOrderByDeadlineAsc(User user);


    List<EmployeeTask> findAllByOrderByDeadlineAsc();

    // Gələcəkdə 1C sinxronizasiyası üçün:
    Optional<EmployeeTask> findByExternal1cId(String external1cId);
}