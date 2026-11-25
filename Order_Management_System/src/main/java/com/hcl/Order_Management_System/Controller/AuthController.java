package com.hcl.Order_Management_System.Controller;

import com.hcl.Order_Management_System.DTO.AuthResponse;
import com.hcl.Order_Management_System.DTO.LoginRequest;
import com.hcl.Order_Management_System.DTO.SetupAdminRequest;
import com.hcl.Order_Management_System.DTO.SetupStatusResponse;
import com.hcl.Order_Management_System.DTO.UserResponse;
import com.hcl.Order_Management_System.Exception.BadRequestException;
import com.hcl.Order_Management_System.Model.User;
import com.hcl.Order_Management_System.Security.JwtService;
import com.hcl.Order_Management_System.Service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserService userService,
            JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @GetMapping("/setup-status")
    public SetupStatusResponse getSetupStatus() {
        return new SetupStatusResponse(userService.isSetupRequired(), userService.getUserCount());
    }

    @PostMapping("/setup-admin")
    public ResponseEntity<AuthResponse> setupInitialAdmin(@RequestBody SetupAdminRequest request) {
        User admin = userService.setupInitialAdmin(request);
        String token = jwtService.generateToken(admin);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, jwtService.getExpirationMs(), UserResponse.from(admin)));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null
                || request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadRequestException("Username and password are required.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername().trim(),
                            request.getPassword()));
        } catch (DisabledException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", 401, "error", "Unauthorized", "message", "Account is deactivated."));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", 401, "error", "Unauthorized", "message", "Invalid username or password."));
        }

        User user = userService.getByUsername(request.getUsername().trim());
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, jwtService.getExpirationMs(), UserResponse.from(user)));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        User user = userService.getByUsername(authentication.getName());
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
