package com.hcl.Order_Management_System.Service;

import com.hcl.Order_Management_System.DTO.CreateUserRequest;
import com.hcl.Order_Management_System.DTO.SetupAdminRequest;
import com.hcl.Order_Management_System.DTO.UpdateUserRequest;
import com.hcl.Order_Management_System.DTO.UserResponse;
import com.hcl.Order_Management_System.Exception.BadRequestException;
import com.hcl.Order_Management_System.Exception.ConflictException;
import com.hcl.Order_Management_System.Exception.ResourceNotFoundException;
import com.hcl.Order_Management_System.Model.Role;
import com.hcl.Order_Management_System.Model.User;
import com.hcl.Order_Management_System.Repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean isSetupRequired() {
        return userRepository.count() == 0;
    }

    public long getUserCount() {
        return userRepository.count();
    }

    @Transactional
    public User setupInitialAdmin(SetupAdminRequest request) {
        if (userRepository.count() > 0) {
            throw new ConflictException("Initial administrator has already been configured.");
        }
        if (request == null) {
            throw new BadRequestException("Setup payload is required.");
        }

        CreateUserRequest createReq = new CreateUserRequest();
        createReq.setUsername(request.getUsername());
        createReq.setPassword(request.getPassword());
        createReq.setFullName(request.getFullName());
        createReq.setEmail(request.getEmail());
        createReq.setRole(Role.ADMIN);

        return createUserEntity(createReq);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        return UserResponse.from(createUserEntity(request));
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return UserResponse.from(user);
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request, String currentUsername) {
        if (request == null) {
            throw new BadRequestException("Update payload is required.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        if (request.getFullName() != null) {
            if (request.getFullName().isBlank()) {
                throw new BadRequestException("Full name cannot be blank.");
            }
            user.setFullName(request.getFullName().trim());
        }

        if (request.getEmail() != null) {
            String trimmedEmail = request.getEmail().trim().toLowerCase();
            if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
                throw new BadRequestException("A valid email address is required.");
            }
            if (!trimmedEmail.equalsIgnoreCase(user.getEmail())
                    && userRepository.existsByEmail(trimmedEmail)) {
                throw new ConflictException("Email is already registered: " + trimmedEmail);
            }
            user.setEmail(trimmedEmail);
        }

        if (request.getRole() != null) {
            if (user.getUsername().equals(currentUsername) && request.getRole() != Role.ADMIN) {
                throw new BadRequestException("You cannot remove your own ADMIN role.");
            }
            user.setRole(request.getRole());
        }

        if (request.getActive() != null) {
            if (user.getUsername().equals(currentUsername) && !request.getActive()) {
                throw new BadRequestException("You cannot deactivate your own account.");
            }
            user.setActive(request.getActive());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            validatePassword(request.getPassword());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        if (user.getUsername().equals(currentUsername)) {
            throw new BadRequestException("You cannot delete your own active account.");
        }

        userRepository.delete(user);
    }

    private User createUserEntity(CreateUserRequest request) {
        if (request == null) {
            throw new BadRequestException("User payload is required.");
        }
        if (request.getUsername() == null || request.getUsername().trim().length() < 3) {
            throw new BadRequestException("Username must be at least 3 characters.");
        }
        String username = request.getUsername().trim();

        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new BadRequestException("Full name is required.");
        }
        if (request.getEmail() == null || request.getEmail().isBlank() || !request.getEmail().contains("@")) {
            throw new BadRequestException("A valid email address is required.");
        }
        String email = request.getEmail().trim().toLowerCase();

        validatePassword(request.getPassword());

        if (request.getRole() == null) {
            throw new BadRequestException("User role is required.");
        }

        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already exists: " + email);
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setRole(request.getRole());
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters long.");
        }
    }
}
