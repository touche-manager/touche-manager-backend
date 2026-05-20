package com.touchemanager.athlete.repository;

import com.touchemanager.athlete.entity.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AthleteRepository extends JpaRepository<Athlete, Long> {

    Optional<Athlete> findByUserId(Long userId);

    boolean existsByDni(String dni);

    Optional<Athlete> findByDni(String dni);
}
