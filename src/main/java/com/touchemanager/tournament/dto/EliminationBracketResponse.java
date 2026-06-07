package com.touchemanager.tournament.dto;

import com.touchemanager.bout.dto.BoutResponse;
import com.touchemanager.bout.entity.EliminationRound;

import java.util.List;
import java.util.Map;

public record EliminationBracketResponse(
        Long tournamentId,
        String tournamentName,
        int tableauSize,
        Map<EliminationRound, List<BoutResponse>> roundBouts
) {}
