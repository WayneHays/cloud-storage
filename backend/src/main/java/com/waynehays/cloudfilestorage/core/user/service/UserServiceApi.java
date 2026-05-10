package com.waynehays.cloudfilestorage.core.user.service;

import com.waynehays.cloudfilestorage.core.user.api.dto.request.SignUpRequest;
import com.waynehays.cloudfilestorage.core.user.api.dto.response.UserResponse;

public interface UserServiceApi {

    UserResponse signUp(SignUpRequest dto);
}
