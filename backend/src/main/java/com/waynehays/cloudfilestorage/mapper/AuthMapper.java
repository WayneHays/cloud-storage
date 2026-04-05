package com.waynehays.cloudfilestorage.mapper;

import com.waynehays.cloudfilestorage.dto.request.auth.SignInRequest;
import com.waynehays.cloudfilestorage.dto.response.UserDto;
import com.waynehays.cloudfilestorage.security.CustomUserDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usedSpace", ignore = true)
    UserDto toUserDto(SignInRequest signInRequest);

    @Mapping(target = "usedSpace", ignore = true)
    UserDto toUserDto(CustomUserDetails userDetails);
}
