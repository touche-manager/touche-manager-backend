package com.touchemanager.tournament.repository;

import com.touchemanager.tournament.entity.RefereeApplication;
import com.touchemanager.tournament.entity.RefereeApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefereeApplicationRepository extends JpaRepository<RefereeApplication, Long> {
    List<RefereeApplication> findByTournamentIdOrderByAppliedAtDesc(Long tournamentId);
    List<RefereeApplication> findByRefereeIdOrderByAppliedAtDesc(Long refereeId);
    Optional<RefereeApplication> findByRefereeIdAndTournamentId(Long refereeId, Long tournamentId);
    boolean existsByRefereeIdAndTournamentId(Long refereeId, Long tournamentId);
    boolean existsByRefereeIdAndTournamentIdAndStatus(Long refereeId, Long tournamentId, RefereeApplicationStatus status);
}
