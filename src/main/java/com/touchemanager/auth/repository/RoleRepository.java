package com.touchemanager.auth.repository;

import com.touchemanager.auth.entity.Role;
import com.touchemanager.auth.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
