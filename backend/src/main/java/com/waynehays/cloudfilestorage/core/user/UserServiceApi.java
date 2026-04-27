package com.waynehays.cloudfilestorage.core.user;

import com.waynehays.cloudfilestorage.core.user.dto.request.SignUpRequest;
import com.waynehays.cloudfilestorage.core.user.dto.response.UserDto;

interface UserServiceApi {

    UserDto signUp(SignUpRequest dto);
}
