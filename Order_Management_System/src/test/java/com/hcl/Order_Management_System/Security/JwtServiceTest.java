package com.hcl.Order_Management_System.Security;

import com.hcl.Order_Management_System.Model.Role;
import com.hcl.Order_Management_System.Model.User;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    @Test
    void generateAndValidateToken_withEphemeralKey_succeedsAndRejectsTampering() {
        JwtService jwtService = new JwtService("", 3600_000L, new ObjectMapper());

        User user = new User();
        user.setUserId(42L);
        user.setUsername("anushka");
        user.setRole(Role.ADMIN);

        String token = jwtService.generateToken(user);
        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);

        JwtService.JwtClaims claims = jwtService.parseAndValidate(token);
        assertNotNull(claims);
        assertEquals("anushka", claims.username());
        assertEquals("ADMIN", claims.role());

        // Tampered payload/signature must fail validation
        assertNull(jwtService.parseAndValidate(token + "tamper"));
    }
}
