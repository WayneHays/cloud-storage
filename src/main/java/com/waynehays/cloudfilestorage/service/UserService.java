package com.waynehays.cloudfilestorage.service;

import com.waynehays.cloudfilestorage.dto.request.SignUpRequest;
import com.waynehays.cloudfilestorage.dto.response.UserDto;

public interface UserService {

    UserDto signUp(SignUpRequest dto);
}
