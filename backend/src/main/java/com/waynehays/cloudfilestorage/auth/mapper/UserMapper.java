package com.waynehays.cloudfilestorage.auth.mapper;

import com.waynehays.cloudfilestorage.auth.dto.request.SignUpRequest;
import com.waynehays.cloudfilestorage.auth.dto.response.UserDto;
import com.waynehays.cloudfilestorage.auth.entity.User;
import com.waynehays.cloudfilestorage.auth.security.CustomUserDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    UserDto toDto(CustomUserDetails userDetails);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(SignUpRequest signUpRequest);
}
