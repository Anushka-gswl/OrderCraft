package com.hcl.Order_Management_System.Service;

import com.hcl.Order_Management_System.DTO.CreateUserRequest;
import com.hcl.Order_Management_System.DTO.SetupAdminRequest;
import com.hcl.Order_Management_System.DTO.UpdateUserRequest;
import com.hcl.Order_Management_System.DTO.UserResponse;
import com.hcl.Order_Management_System.Exception.BadRequestException;
import com.hcl.Order_Management_System.Exception.ConflictException;
import com.hcl.Order_Management_System.Model.Role;
import com.hcl.Order_Management_System.Model.User;
import com.hcl.Order_Management_System.Repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private UserService userService;

    @Test
    void setupInitialAdmin_whenNoUsers_hashesPasswordAndCreatesAdmin() {
        SetupAdminRequest req = new SetupAdminRequest();
        req.setUsername("sysadmin");
        req.setPassword("StrongPass123");
        req.setFullName("System Admin");
        req.setEmail("admin@ordercraft.local");

        when(userRepository.count()).thenReturn(0L);
        when(userRepository.existsByUsername("sysadmin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@ordercraft.local")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setUserId(1L);
            return u;
        });

        User created = userService.setupInitialAdmin(req);

        assertEquals(Role.ADMIN, created.getRole());
        assertNotEquals("StrongPass123", created.getPasswordHash());
        assertTrue(passwordEncoder.matches("StrongPass123", created.getPasswordHash()));
    }

    @Test
    void setupInitialAdmin_whenUsersExist_throwsConflict() {
        when(userRepository.count()).thenReturn(1L);

        assertThrows(ConflictException.class,
                () -> userService.setupInitialAdmin(new SetupAdminRequest()));
    }

    @Test
    void updateUser_preventsRemovingOwnAdminRoleOrDeactivatingSelf() {
        User admin = new User();
        admin.setUserId(1L);
        admin.setUsername("sysadmin");
        admin.setEmail("admin@ordercraft.local");
        admin.setRole(Role.ADMIN);
        admin.setActive(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        UpdateUserRequest demoteSelf = new UpdateUserRequest();
        demoteSelf.setRole(Role.SALES_EXECUTIVE);
        assertThrows(BadRequestException.class,
                () -> userService.updateUser(1L, demoteSelf, "sysadmin"));

        UpdateUserRequest deactivateSelf = new UpdateUserRequest();
        deactivateSelf.setActive(false);
        assertThrows(BadRequestException.class,
                () -> userService.updateUser(1L, deactivateSelf, "sysadmin"));
    }

    @Test
    void createUser_duplicateUsername_throwsConflict() {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("sales1");
        req.setPassword("Secret123");
        req.setFullName("Sales One");
        req.setEmail("sales1@ordercraft.local");
        req.setRole(Role.SALES_EXECUTIVE);

        when(userRepository.existsByUsername("sales1")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.createUser(req));
    }
}
