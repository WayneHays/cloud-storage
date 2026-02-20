package com.waynehays.cloudfilestorage.service;

import com.waynehays.cloudfilestorage.dto.auth.request.SignUpRequest;
import com.waynehays.cloudfilestorage.dto.auth.response.UserDto;

public interface UserService {

    UserDto signUp(SignUpRequest dto);
}
