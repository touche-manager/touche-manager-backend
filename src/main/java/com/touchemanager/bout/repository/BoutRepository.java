package com.touchemanager.bout.repository;

import com.touchemanager.bout.entity.Bout;
import com.touchemanager.bout.entity.BoutStatus;
import com.touchemanager.bout.entity.EliminationRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

    List<Bout> findByPouleIdOrderByBoutOrderAsc(Long pouleId);

    Optional<Bout> findByPouleIdAndBoutOrder(Long pouleId, Integer boutOrder);

    @Query("SELECT b FROM Bout b WHERE " +
           "(b.athleteLeft.id = :athleteId OR b.athleteRight.id = :athleteId) " +
           "ORDER BY b.id DESC")
    List<Bout> findByAthleteId(@Param("athleteId") Long athleteId);

    List<Bout> findByTournamentIdAndPouleIsNullOrderByEliminationRoundAscBracketPositionAsc(Long tournamentId);

    List<Bout> findByTournamentIdAndPouleIsNullAndRefereesId(Long tournamentId, Long refereeId);

    Optional<Bout> findByTournamentIdAndEliminationRoundAndBracketPosition(
            Long tournamentId, EliminationRound eliminationRound, int bracketPosition);

    @Query("SELECT DISTINCT b FROM Bout b LEFT JOIN b.poule p LEFT JOIN p.referees pr LEFT JOIN b.referees br " +
           "WHERE b.tournament.id = :tournamentId AND (pr.id = :refereeId OR br.id = :refereeId) " +
           "ORDER BY b.id ASC")
    List<Bout> findAssignedBouts(@Param("tournamentId") Long tournamentId, @Param("refereeId") Long refereeId);

    /** Used by the public spectator endpoint to list all in-progress bouts */
    List<Bout> findByStatusOrderByStartedAtDesc(BoutStatus status);
}
