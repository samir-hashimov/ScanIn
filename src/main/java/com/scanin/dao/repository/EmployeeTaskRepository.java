package com.scanin.dao.repository;

import com.scanin.dao.entity.EmployeeTask;
import com.scanin.dao.entity.User;
import com.scanin.util.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmployeeTaskRepository extends JpaRepository<EmployeeTask, Long> {

    @Query("select t from EmployeeTask t join fetch t.user left join fetch t.pausedFromTask where t.user = :user order by t.deadline asc")
    List<EmployeeTask> findAllByUserOrderByDeadlineAsc(@Param("user") User user);

    @Query("select t from EmployeeTask t join fetch t.user left join fetch t.pausedFromTask order by t.deadline asc")
    List<EmployeeTask> findAllByOrderByDeadlineAsc();

    List<EmployeeTask> findAllByUserAndStatusIn(User user, Collection<TaskStatus> statuses);

    Optional<EmployeeTask> findByExternal1cId(String external1cId);
}
