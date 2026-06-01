package com.touchemanager.bout.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "bout_events")
@Getter
@Setter
@NoArgsConstructor
public class BoutEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bout_id", nullable = false)
    private Bout bout;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 10)
    private EventSide side;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private EventType eventType;

    // Points added to the indicated side (typically 1 for TOUCHE/PENALTY, 0 for CARD)
    @Column(name = "score_delta", nullable = false)
    private int scoreDelta;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "referee_email", length = 255)
    private String refereeEmail;
}
