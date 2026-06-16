package com.touchemanager.tournament.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for the FIE poule bout-ordering tables.
 *
 * Guards against malformed tables like the one that shipped for poule size 6,
 * which repeated 3 pairs and omitted 3 others — producing 15 bouts but only
 * 12 unique pairings, so some fencers met twice and others never met.
 *
 * A valid table for N fencers must contain exactly C(N,2) bouts, covering each
 * unordered pair of positions {1..N} exactly once.
 */
class FieBoutOrderTest {

    @ParameterizedTest(name = "poule of {0} fencers covers every pair exactly once")
    @ValueSource(ints = {4, 5, 6, 7})
    @DisplayName("FIE bout order covers C(n,2) unique pairs with no repeats or omissions")
    void boutOrderCoversAllPairsOnce(int n) {
        int[][] pairings = PouleServiceImpl.getFieBoutOrder(n);

        int expected = n * (n - 1) / 2;
        assertThat(pairings.length).isEqualTo(expected);

        Set<String> seen = new HashSet<>();
        for (int[] pair : pairings) {
            assertThat(pair[0]).isBetween(1, n);
            assertThat(pair[1]).isBetween(1, n);
            assertThat(pair[0]).isNotEqualTo(pair[1]);

            int lo = Math.min(pair[0], pair[1]);
            int hi = Math.max(pair[0], pair[1]);
            assertThat(seen.add(lo + "-" + hi))
                    .as("pair %d-%d must appear only once", lo, hi)
                    .isTrue();
        }

        assertThat(seen).hasSize(expected);
    }
}
