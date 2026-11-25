package com.hcl.Order_Management_System.Security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/setup-status", "/api/auth/setup-admin")
                        .permitAll()
                        .requestMatchers("/api/auth/me").authenticated()

                        // User management: ADMIN only
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        // Customers: ADMIN, SALES_EXECUTIVE
                        .requestMatchers("/api/customers/**")
                        .hasAnyRole("ADMIN", "SALES_EXECUTIVE")

                        // Products & BOM: read for all operational roles, write for ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/product-materials/**")
                        .hasAnyRole("ADMIN", "SALES_EXECUTIVE", "INVENTORY_MANAGER", "PROCUREMENT")
                        .requestMatchers("/api/products/**", "/api/product-materials/**")
                        .hasRole("ADMIN")

                        // Materials: read for ADMIN/INVENTORY_MANAGER/PROCUREMENT, write for ADMIN/INVENTORY_MANAGER
                        .requestMatchers(HttpMethod.GET, "/api/materials/**")
                        .hasAnyRole("ADMIN", "INVENTORY_MANAGER", "PROCUREMENT")
                        .requestMatchers("/api/materials/**")
                        .hasAnyRole("ADMIN", "INVENTORY_MANAGER")

                        // Inventory: read for all roles, write for ADMIN/INVENTORY_MANAGER
                        .requestMatchers(HttpMethod.GET, "/api/inventory/**")
                        .hasAnyRole("ADMIN", "INVENTORY_MANAGER", "PROCUREMENT", "SALES_EXECUTIVE")
                        .requestMatchers("/api/inventory/**")
                        .hasAnyRole("ADMIN", "INVENTORY_MANAGER")

                        // Customer Orders & Workflow
                        .requestMatchers(HttpMethod.GET, "/api/orders/**", "/api/order-items/**")
                        .hasAnyRole("ADMIN", "SALES_EXECUTIVE", "INVENTORY_MANAGER", "PROCUREMENT")
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/create-purchase-order")
                        .hasAnyRole("ADMIN", "PROCUREMENT", "INVENTORY_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/start-processing", "/api/orders/*/complete")
                        .hasAnyRole("ADMIN", "INVENTORY_MANAGER")
                        .requestMatchers("/api/orders/**", "/api/order-items/**")
                        .hasAnyRole("ADMIN", "SALES_EXECUTIVE")

                        // Purchase Orders
                        .requestMatchers(HttpMethod.GET, "/api/purchase-orders/**", "/api/purchase-order-items/**")
                        .hasAnyRole("ADMIN", "PROCUREMENT", "INVENTORY_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/purchase-orders/*/receive")
                        .hasAnyRole("ADMIN", "INVENTORY_MANAGER")
                        .requestMatchers("/api/purchase-orders/**", "/api/purchase-order-items/**")
                        .hasAnyRole("ADMIN", "PROCUREMENT")

                        // Invoices: ADMIN + SALES_EXECUTIVE can read, generate, update and delete
                        .requestMatchers(HttpMethod.GET, "/api/invoices/**")
                        .hasAnyRole("ADMIN", "SALES_EXECUTIVE")
                        .requestMatchers("/api/invoices/**")
                        .hasAnyRole("ADMIN", "SALES_EXECUTIVE")

                        // Dashboard summary: all authenticated operational roles
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/**")
                        .hasAnyRole("ADMIN", "SALES_EXECUTIVE", "INVENTORY_MANAGER", "PROCUREMENT")

                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", LocalDateTime.now().toString());
            body.put("status", 401);
            body.put("error", "Unauthorized");
            body.put("message", "Authentication is required to access this resource.");
            body.put("path", request.getRequestURI());
            response.getWriter().write(objectMapper.writeValueAsString(body));
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", LocalDateTime.now().toString());
            body.put("status", 403);
            body.put("error", "Forbidden");
            body.put("message", "You do not have permission to perform this action.");
            body.put("path", request.getRequestURI());
            response.getWriter().write(objectMapper.writeValueAsString(body));
        };
    }
}