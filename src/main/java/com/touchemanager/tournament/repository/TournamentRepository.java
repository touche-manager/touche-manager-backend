package com.touchemanager.tournament.repository;

import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.tournament.entity.Category;
import com.touchemanager.tournament.entity.Tournament;
import com.touchemanager.tournament.entity.TournamentPhase;
import com.touchemanager.tournament.entity.Weapon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Tournament JPA repository.
 *
 * Extends {@link JpaSpecificationExecutor} to allow dynamic filter queries
 * via {@link TournamentSpecification} — avoids PostgreSQL typed-NULL errors
 * that arise from using nullable @Param values in @Query JPQL.
 */
@Repository
public interface TournamentRepository
        extends JpaRepository<Tournament, Long>,
                JpaSpecificationExecutor<Tournament> {

    List<Tournament> findAllByOrderByDateAsc();
    List<Tournament> findByCreatedByIdOrderByDateAsc(Long createdByUserId);
    long countByPhase(TournamentPhase phase);

    @Query("SELECT t FROM Tournament t WHERE t.phase = 'FINISHED' " +
           "AND t.category = :category " +
           "AND t.gender = :gender " +
           "AND t.weapon = :weapon " +
           "ORDER BY t.date DESC " +
           "LIMIT 4")
    List<Tournament> findTop4ByDisciplineOrderByDateDesc(
            @Param("category") Category category,
            @Param("gender") Gender gender,
            @Param("weapon") Weapon weapon);
}
