package com.touchemanager.tournament.service.impl;

import com.touchemanager.tournament.dto.PouleStandingEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the poule standings sorting comparator logic.
 *
 * Regression tests that guard against the Java Comparator chained-reversed() bug
 * where thenComparingInt(x).reversed() would invert the entire chain instead of
 * only the last comparator, causing incorrect rankings for tied athletes.
 *
 * Correct ordering: victories DESC → indicator DESC → touchesScored DESC
 */
class PouleStandingsSortTest {

    /**
     * The corrected comparator extracted from PouleServiceImpl#computePouleStandings.
     * This is tested in isolation so any future regression is immediately visible.
     */
    private static final Comparator<PouleStandingEntry> STANDINGS_COMPARATOR =
            Comparator.<PouleStandingEntry>comparingInt(PouleStandingEntry::victories).reversed()
                    .thenComparing(Comparator.comparingInt(PouleStandingEntry::indicator).reversed())
                    .thenComparing(Comparator.comparingInt(PouleStandingEntry::touchesScored).reversed());

    // ── Helper ──────────────────────────────────────────────────────────────────

    private PouleStandingEntry entry(long id, String name, int victories, int ta, int tr) {
        return new PouleStandingEntry(id, name, null, 1, victories, ta, tr, ta - tr);
    }

    private List<PouleStandingEntry> sorted(PouleStandingEntry... entries) {
        return Arrays.stream(entries).sorted(STANDINGS_COMPARATOR).toList();
    }

    // ── Tests ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Higher victories ranks first")
    void victories_descending() {
        var a = entry(1, "3 wins",  3, 15, 10);
        var b = entry(2, "2 wins",  2, 14, 10);
        var c = entry(3, "1 win",   1, 12, 11);

        var result = sorted(c, b, a);

        assertThat(result).extracting(PouleStandingEntry::fullName)
                .containsExactly("3 wins", "2 wins", "1 win");
    }

    @Test
    @DisplayName("Tie in victories: higher indicator ranks first (Fernandez > Martinez case)")
    void tieOnVictories_higherIndicatorRanksFirst() {
        // Santiago: 2V, TA=14, TR=11 → Ind=+3
        // Ignacio:  2V, TA=14, TR=10 → Ind=+4  ← should rank higher
        var martinez  = entry(1, "Santiago Martínez",  2, 14, 11);
        var fernandez = entry(2, "Ignacio Fernández",   2, 14, 10);

        var result = sorted(martinez, fernandez);

        assertThat(result.get(0).fullName()).isEqualTo("Ignacio Fernández");
        assertThat(result.get(1).fullName()).isEqualTo("Santiago Martínez");
    }

    @Test
    @DisplayName("Tie in victories: negative indicator ranks below positive indicator")
    void tieOnVictories_negativeIndicatorRanksLast() {
        // Pérez:    1V, TA=12, TR=13 → Ind=-1
        // Rodríguez:1V, TA=12, TR=11 → Ind=+1  ← should rank higher
        var perez      = entry(1, "Tomás Pérez",    1, 12, 13);
        var rodriguez  = entry(2, "Matías Rodríguez", 1, 12, 11);

        var result = sorted(perez, rodriguez);

        assertThat(result.get(0).fullName()).isEqualTo("Matías Rodríguez");
        assertThat(result.get(1).fullName()).isEqualTo("Tomás Pérez");
    }

    @Test
    @DisplayName("Tie in victories and indicator: higher touchesScored ranks first")
    void tieOnVictoriesAndIndicator_higherTouchesRanksFirst() {
        // Both 0V, Ind=-8 but A scored 8 and B scored 7
        var a = entry(1, "More touches", 0, 8, 16);  // ind = -8
        var b = entry(2, "Less touches", 0, 7, 15);  // ind = -8

        var result = sorted(b, a);

        assertThat(result.get(0).fullName()).isEqualTo("More touches");
    }

    @Test
    @DisplayName("Tie in victories: López (0V, -9) ranks below Sánchez (0V, -8)")
    void tieOnZeroVictories_sanchezBeforeLopez() {
        // From the screenshot regression: Sánchez -8 should be above López -9
        var sanchez = entry(1, "Agustín Sánchez", 0, 7, 15);  // ind = -8
        var lopez   = entry(2, "Nicolás López",   0, 6, 15);  // ind = -9

        var result = sorted(lopez, sanchez);

        assertThat(result.get(0).fullName()).isEqualTo("Agustín Sánchez");
        assertThat(result.get(1).fullName()).isEqualTo("Nicolás López");
    }

    @Test
    @DisplayName("Full classification matches expected order from the screenshot regression")
    void fullClassification_matchesScreenshot() {
        var gomez     = entry(1, "Carlos Gómez",      3, 15, 10);  // ind=+5
        var garcia    = entry(2, "Facundo García",     3, 15, 10);  // ind=+5
        var martinez  = entry(3, "Santiago Martínez",  2, 14, 11);  // ind=+3
        var fernandez = entry(4, "Ignacio Fernández",  2, 14, 10);  // ind=+4  ← should beat martinez
        var perez     = entry(5, "Tomás Pérez",        1, 12, 13);  // ind=-1
        var rodriguez = entry(6, "Matías Rodríguez",   1, 12, 11);  // ind=+1  ← should beat perez
        var lopez     = entry(7, "Nicolás López",      0,  6, 15);  // ind=-9
        var sanchez   = entry(8, "Agustín Sánchez",    0,  7, 15);  // ind=-8  ← should beat lopez

        var result = sorted(gomez, garcia, martinez, fernandez, perez, rodriguez, lopez, sanchez);

        // Positions 3/4 corrected: Fernández before Martínez
        assertThat(result.get(2).fullName()).isEqualTo("Ignacio Fernández");
        assertThat(result.get(3).fullName()).isEqualTo("Santiago Martínez");
        // Positions 5/6 corrected: Rodríguez before Pérez
        assertThat(result.get(4).fullName()).isEqualTo("Matías Rodríguez");
        assertThat(result.get(5).fullName()).isEqualTo("Tomás Pérez");
        // Positions 7/8 corrected: Sánchez before López
        assertThat(result.get(6).fullName()).isEqualTo("Agustín Sánchez");
        assertThat(result.get(7).fullName()).isEqualTo("Nicolás López");
    }
}
