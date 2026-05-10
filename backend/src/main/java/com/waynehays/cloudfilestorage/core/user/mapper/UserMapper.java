package com.waynehays.cloudfilestorage.core.user.mapper;

import com.waynehays.cloudfilestorage.core.user.api.dto.response.UserResponse;
import com.waynehays.cloudfilestorage.core.user.entity.User;
import com.waynehays.cloudfilestorage.infrastructure.security.CustomUserDetails;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(CustomUserDetails userDetails);

    UserResponse toResponse(User user);
}
