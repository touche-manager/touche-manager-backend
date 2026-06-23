package com.touchemanager.bout.entity;

public enum EventType {
    TOUCHE,                // Valid touch — scores +1 for this fencer
    YELLOW_CARD,           // Warning — no score change; tracked for escalation to RED_CARD
    RED_CARD,              // Infraction — scores +1 for the OPPONENT of the card receiver
    SCORE_CORRECTION,      // Manual correction by referee — removes 1 point from this fencer (min 0)
    YELLOW_CARD_REMOVAL,   // Removes the last yellow card from this fencer (no score change)
    RED_CARD_REMOVAL       // Removes the last red card from this fencer AND subtracts 1 from the opponent's score (min 0)
}
