package com.waynehays.cloudfilestorage.mapper;

import com.waynehays.cloudfilestorage.dto.auth.response.UserDto;
import com.waynehays.cloudfilestorage.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);
}
