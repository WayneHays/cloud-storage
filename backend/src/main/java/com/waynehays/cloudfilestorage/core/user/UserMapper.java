package com.waynehays.cloudfilestorage.core.user;

import com.waynehays.cloudfilestorage.core.user.api.dto.SignUpRequest;
import com.waynehays.cloudfilestorage.core.user.dto.response.UserDto;
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
