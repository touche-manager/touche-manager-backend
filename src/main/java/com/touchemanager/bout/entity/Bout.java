package com.touchemanager.bout.entity;

import com.touchemanager.athlete.entity.Athlete;
import com.touchemanager.tournament.entity.Tournament;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bouts")
@Getter
@Setter
@NoArgsConstructor
public class Bout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_left_id", nullable = false)
    private Athlete athleteLeft;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_right_id", nullable = false)
    private Athlete athleteRight;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 20)
    private BoutFormat format;

    @Column(name = "score_left", nullable = false)
    private int scoreLeft = 0;

    @Column(name = "score_right", nullable = false)
    private int scoreRight = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BoutStatus status = BoutStatus.PENDING;

    @Column(name = "current_period", nullable = false)
    private int currentPeriod = 1;

    // Total seconds elapsed across all periods
    @Column(name = "elapsed_seconds", nullable = false)
    private int elapsedSeconds = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Athlete winner;

    @Column(name = "referee_email", length = 255)
    private String refereeEmail;

    @OneToMany(mappedBy = "bout", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoutEvent> events = new ArrayList<>();
}
