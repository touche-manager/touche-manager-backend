package com.touchemanager.tournament.repository;

import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.tournament.entity.Category;
import com.touchemanager.tournament.entity.Tournament;
import com.touchemanager.tournament.entity.Weapon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    List<Tournament> findAllByOrderByDateAsc();
    List<Tournament> findByCreatedByIdOrderByDateAsc(Long createdByUserId);

    @Query("SELECT t FROM Tournament t WHERE t.phase = 'FINISHED' " +
           "AND (:category IS NULL OR t.category = :category) " +
           "AND (:gender IS NULL OR t.gender = :gender) " +
           "AND (:weapon IS NULL OR t.weapon = :weapon) " +
           "AND (:dateFrom IS NULL OR t.date >= :dateFrom) " +
           "AND (:dateTo IS NULL OR t.date <= :dateTo) " +
           "ORDER BY t.date DESC")
    List<Tournament> findFinishedTournaments(
            @Param("category") Category category,
            @Param("gender") Gender gender,
            @Param("weapon") Weapon weapon,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo);

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

