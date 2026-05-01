package com.waynehays.cloudfilestorage.core.user;

import com.waynehays.cloudfilestorage.core.user.api.dto.SignUpRequest;
import com.waynehays.cloudfilestorage.core.user.dto.response.UserDto;

public interface UserServiceApi {

    UserDto signUp(SignUpRequest dto);
}
