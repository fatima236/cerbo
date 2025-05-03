package com.example.cerbo.repository;



import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE :roleName MEMBER OF u.roles")
    List<User> findByRolesName(@Param("roleName") String roleName);

    // In UserRepository
    List<User> findByRolesContaining(String role);

}