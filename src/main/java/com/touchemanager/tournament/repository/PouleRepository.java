package com.touchemanager.tournament.repository;

import com.touchemanager.tournament.entity.Poule;
import com.touchemanager.tournament.entity.PouleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PouleRepository extends JpaRepository<Poule, Long> {
    List<Poule> findByTournamentIdOrderByNumberAsc(Long tournamentId);
    List<Poule> findByTournamentIdAndStatusOrderByNumberAsc(Long tournamentId, PouleStatus status);
    boolean existsByTournamentId(Long tournamentId);
    
    @Query("SELECT p FROM Poule p JOIN p.referees r WHERE r.id = :userId AND p.tournament.id = :tournamentId ORDER BY p.number ASC")
    List<Poule> findByRefereeIdAndTournamentId(@Param("userId") Long userId, @Param("tournamentId") Long tournamentId);
}
