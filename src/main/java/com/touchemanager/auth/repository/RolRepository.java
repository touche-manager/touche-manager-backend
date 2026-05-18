package com.touchemanager.auth.repository;

import com.touchemanager.auth.entity.NombreRol;
import com.touchemanager.auth.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombre(NombreRol nombre);
}
