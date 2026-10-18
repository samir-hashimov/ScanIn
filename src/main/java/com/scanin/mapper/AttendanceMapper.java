package com.scanin.mapper;

import com.scanin.dao.entity.Attendance;
import com.scanin.dto.response.AttendanceReportResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AttendanceMapper {

    @Mapping(source = "id", target = "attendanceId")
    @Mapping(source = "user.employeeCode", target = "employeeCode")
    @Mapping(source = "user.fullName", target = "fullName")
    @Mapping(source = "user.position", target = "position")
    AttendanceReportResponse toReportResponse(Attendance attendance);

    List<AttendanceReportResponse> toReportResponseList(List<Attendance> attendances);
}