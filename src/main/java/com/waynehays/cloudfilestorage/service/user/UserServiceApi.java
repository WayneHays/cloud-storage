package com.waynehays.cloudfilestorage.service.user;

import com.waynehays.cloudfilestorage.dto.request.auth.SignUpRequest;
import com.waynehays.cloudfilestorage.dto.response.UserDto;

public interface UserServiceApi {

    UserDto signUp(SignUpRequest dto);
}
