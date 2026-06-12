package com.touchemanager.tournament.repository;

import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.tournament.entity.Category;
import com.touchemanager.tournament.entity.Tournament;
import com.touchemanager.tournament.entity.TournamentPhase;
import com.touchemanager.tournament.entity.Weapon;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * JPA Specification factory for Tournament filtering.
 *
 * Using Specification instead of a @Query with nullable parameters avoids the
 * PostgreSQL "could not determine data type of parameter $N" error that occurs
 * when Hibernate passes typed NULL values for unused filter params.
 *
 * Each method returns a predicate that is only added to the query when the
 * corresponding filter value is non-null (see {@link #finishedWithFilters}).
 */
public class TournamentSpecification {

    private TournamentSpecification() {}

    /** Matches only FINISHED tournaments. */
    public static Specification<Tournament> isFinished() {
        return (root, query, cb) ->
                cb.equal(root.get("phase"), TournamentPhase.FINISHED);
    }

    public static Specification<Tournament> hasCategory(Category category) {
        return (root, query, cb) ->
                cb.equal(root.get("category"), category);
    }

    public static Specification<Tournament> hasGender(Gender gender) {
        return (root, query, cb) ->
                cb.equal(root.get("gender"), gender);
    }

    public static Specification<Tournament> hasWeapon(Weapon weapon) {
        return (root, query, cb) ->
                cb.equal(root.get("weapon"), weapon);
    }

    public static Specification<Tournament> dateFrom(LocalDate from) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("date"), from);
    }

    public static Specification<Tournament> dateTo(LocalDate to) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("date"), to);
    }

    public static Specification<Tournament> hasPhase(TournamentPhase phase) {
        return (root, query, cb) ->
                cb.equal(root.get("phase"), phase);
    }

    /**
     * Public tournament search: any phase unless one is given.
     * Only non-null filters are added as predicates.
     */
    public static Specification<Tournament> publicSearch(
            TournamentPhase phase,
            Category category,
            Gender gender,
            Weapon weapon,
            LocalDate dateFrom,
            LocalDate dateTo) {

        Specification<Tournament> spec = (root, query, cb) -> cb.conjunction();

        if (phase    != null) spec = spec.and(hasPhase(phase));
        if (category != null) spec = spec.and(hasCategory(category));
        if (gender   != null) spec = spec.and(hasGender(gender));
        if (weapon   != null) spec = spec.and(hasWeapon(weapon));
        if (dateFrom != null) spec = spec.and(dateFrom(dateFrom));
        if (dateTo   != null) spec = spec.and(dateTo(dateTo));

        return spec;
    }

    /**
     * Composes a single Specification from all provided (nullable) filters.
     * Only non-null filters are added as predicates — avoids the PostgreSQL
     * typed-NULL issue entirely.
     */
    public static Specification<Tournament> finishedWithFilters(
            Category category,
            Gender gender,
            Weapon weapon,
            LocalDate dateFrom,
            LocalDate dateTo) {

        Specification<Tournament> spec = isFinished();

        if (category != null) spec = spec.and(hasCategory(category));
        if (gender   != null) spec = spec.and(hasGender(gender));
        if (weapon   != null) spec = spec.and(hasWeapon(weapon));
        if (dateFrom != null) spec = spec.and(dateFrom(dateFrom));
        if (dateTo   != null) spec = spec.and(dateTo(dateTo));

        return spec;
    }
}
