package com.waynehays.cloudfilestorage.service.user;

import com.waynehays.cloudfilestorage.dto.request.auth.SignUpRequest;
import com.waynehays.cloudfilestorage.dto.response.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserServiceApi {

    UserDto signUp(SignUpRequest dto);

    Page<UserDto> findAll(Pageable pageable);
}
