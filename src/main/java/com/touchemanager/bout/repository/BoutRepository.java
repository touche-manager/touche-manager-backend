package com.touchemanager.bout.repository;

import com.touchemanager.bout.entity.Bout;
import com.touchemanager.bout.entity.BoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoutRepository extends JpaRepository<Bout, Long> {

    List<Bout> findByTournamentIdOrderByIdAsc(Long tournamentId);

    @Query("SELECT b FROM Bout b WHERE b.tournament.id = :tournamentId AND b.status = :status")
    List<Bout> findByTournamentIdAndStatus(@Param("tournamentId") Long tournamentId,
                                           @Param("status") BoutStatus status);

    @Query("SELECT b FROM Bout b WHERE " +
           "(b.athleteLeft.id = :athleteId OR b.athleteRight.id = :athleteId) " +
           "AND b.tournament.id = :tournamentId")
    List<Bout> findByAthleteAndTournament(@Param("athleteId") Long athleteId,
                                          @Param("tournamentId") Long tournamentId);
}
