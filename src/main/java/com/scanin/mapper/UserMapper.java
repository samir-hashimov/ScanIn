package com.scanin.mapper;

import com.scanin.dao.entity.User;
import com.scanin.dto.request.CreateUserRequest;
import com.scanin.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employeeCode", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "password", ignore = true)
    User toEntity(CreateUserRequest request);
}