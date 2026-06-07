package com.touchemanager.bout.entity;
public enum EliminationRound {
    ROUND_OF_64,
    ROUND_OF_32,
    ROUND_OF_16,
    QUARTERFINAL,
    SEMIFINAL,
    FINAL;
    // No BRONZE: both semifinal losers share 3rd place

    /** Returns the next round in the elimination bracket, or null if this is the FINAL. */
    public EliminationRound getNext() {
        return switch (this) {
            case ROUND_OF_64 -> ROUND_OF_32;
            case ROUND_OF_32 -> ROUND_OF_16;
            case ROUND_OF_16 -> QUARTERFINAL;
            case QUARTERFINAL -> SEMIFINAL;
            case SEMIFINAL -> FINAL;
            case FINAL -> null;
        };
    }
}
