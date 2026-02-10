package com.waynehays.cloudfilestorage.service;

import com.waynehays.cloudfilestorage.dto.request.RegistrationDto;
import com.waynehays.cloudfilestorage.dto.response.UserDto;

public interface UserService {

    UserDto register(RegistrationDto dto);
}
