package com.waynehays.cloudfilestorage.mapper;

import com.waynehays.cloudfilestorage.dto.request.auth.SignInRequest;
import com.waynehays.cloudfilestorage.dto.response.UserDto;
import com.waynehays.cloudfilestorage.security.CustomUserDetails;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    UserDto toUserDto(SignInRequest signInRequest);

    UserDto toUserDto(CustomUserDetails userDetails);
}
