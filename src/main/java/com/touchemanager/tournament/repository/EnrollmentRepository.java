package com.touchemanager.tournament.repository;

import com.touchemanager.tournament.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByAthleteId(Long athleteId);
    Optional<Enrollment> findByAthleteIdAndTournamentId(Long athleteId, Long tournamentId);
    List<Enrollment> findByTournamentId(Long tournamentId);

    /**
     * True if the athlete has at least one non-cancelled enrollment in a tournament
     * that has not finished yet. Used to lock profile edits while competing.
     */
    @Query("SELECT COUNT(e) > 0 FROM Enrollment e " +
           "WHERE e.athlete.id = :athleteId " +
           "AND e.status <> com.touchemanager.tournament.entity.EnrollmentStatus.CANCELLED " +
           "AND e.tournament.phase <> com.touchemanager.tournament.entity.TournamentPhase.FINISHED")
    boolean hasActiveEnrollmentInUnfinishedTournament(@Param("athleteId") Long athleteId);
}

