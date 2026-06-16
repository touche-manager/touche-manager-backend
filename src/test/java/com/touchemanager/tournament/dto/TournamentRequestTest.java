package com.touchemanager.tournament.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for the National Championship flag binding.
 *
 * Lombok names the boolean accessors isNational()/setNational(), so without an
 * explicit @JsonProperty Jackson would bind the JSON key "national" and silently
 * drop the "isNational" sent by the frontend — leaving every tournament non-national.
 */
class TournamentRequestTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Test
    @DisplayName("isNational=true in the JSON body is bound onto the request")
    void deserializesIsNationalTrue() throws Exception {
        String json = """
                {
                  "name": "Campeonato Nacional",
                  "weapon": "FOIL",
                  "category": "SENIOR",
                  "gender": "MALE",
                  "location": "CENARD",
                  "date": "2026-12-01",
                  "basePrice": 1000,
                  "isNational": true
                }
                """;

        TournamentRequest request = mapper.readValue(json, TournamentRequest.class);

        assertThat(request.isNational()).isTrue();
    }

    @Test
    @DisplayName("isNational defaults to false when absent")
    void defaultsToFalseWhenAbsent() throws Exception {
        String json = """
                {
                  "name": "Open Regional",
                  "weapon": "EPEE",
                  "category": "CADET",
                  "gender": "FEMALE",
                  "location": "Club",
                  "date": "2026-12-01",
                  "basePrice": 500
                }
                """;

        TournamentRequest request = mapper.readValue(json, TournamentRequest.class);

        assertThat(request.isNational()).isFalse();
    }
}
