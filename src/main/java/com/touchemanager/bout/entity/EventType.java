package com.touchemanager.bout.entity;

public enum EventType {
    TOUCHE,            // Valid touch — scores +1 for this fencer
    YELLOW_CARD,       // Warning — no score change; tracked for escalation to RED_CARD
    RED_CARD,          // Infraction — scores +1 for the OPPONENT of the card receiver
    SCORE_CORRECTION   // Manual correction by referee — removes 1 point from this fencer (min 0)
}
