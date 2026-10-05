package com.hrms.backend.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hrms.backend.dto.UserCreationDto;
import com.hrms.backend.entity.Role;
import com.hrms.backend.entity.User;
import com.hrms.backend.repository.RoleRepository;
import com.hrms.backend.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // Manual Constructor Injection (Bypassing Lombok @RequiredArgsConstructor)
    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public User createUserWithRole(UserCreationDto dto) {
        Role selectedRole = roleRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found with ID: " + dto.getRoleId()));

        User newUser = new User();
        newUser.setFullName(dto.getFullName());
        newUser.setEmail(dto.getEmail());
        newUser.setPassword(passwordEncoder.encode(dto.getPassword())); // BCrypt Hash
        newUser.setRole(selectedRole);

        return userRepository.save(newUser);
    }
}