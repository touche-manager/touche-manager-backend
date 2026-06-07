package com.touchemanager.tournament.repository;

import com.touchemanager.tournament.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByAthleteId(Long athleteId);
    Optional<Enrollment> findByAthleteIdAndTournamentId(Long athleteId, Long tournamentId);
    List<Enrollment> findByTournamentId(Long tournamentId);
}

