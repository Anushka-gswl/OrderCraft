package com.hcl.Order_Management_System.Repository;

import com.hcl.Order_Management_System.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findAllByOrderByCreatedAtDesc();
}
