package com.waynehays.cloudfilestorage.auth.service;

import com.waynehays.cloudfilestorage.auth.dto.request.SignUpRequest;
import com.waynehays.cloudfilestorage.auth.dto.response.UserDto;

public interface UserServiceApi {

    UserDto signUp(SignUpRequest dto);
}
