package com.touchemanager.tournament.service;

import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.tournament.dto.RankingEntryResponse;
import com.touchemanager.tournament.entity.Category;
import com.touchemanager.tournament.entity.Weapon;

import java.util.List;

public interface RankingService {

    /**
     * Returns the ranking for a given discipline (category + gender + weapon),
     * based on the last 4 finished tournaments of that discipline.
     * The worst result per athlete is discarded; the best 3 are summed.
     * National championships apply a 1.2 coefficient.
     */
    List<RankingEntryResponse> getRankings(Category category, Gender gender, Weapon weapon);
}
