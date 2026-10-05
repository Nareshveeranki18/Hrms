package com.hrms.backend.service;

import com.hrms.backend.entity.User;
import com.hrms.backend.entity.Role;
import com.hrms.backend.dto.UserCreationDto;
import java.util.List;

public interface UserService {
    User saveUser(User user);
    List<User> getAllUsers();
    List<Role> getAllRoles();
    User createUserWithRole(UserCreationDto dto);
}