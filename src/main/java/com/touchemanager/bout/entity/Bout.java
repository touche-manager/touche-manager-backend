package com.touchemanager.bout.entity;

import com.touchemanager.athlete.entity.Athlete;
import com.touchemanager.auth.entity.User;
import com.touchemanager.tournament.entity.Poule;
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

    /** Null for elimination bouts */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poule_id")
    private Poule poule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "athlete_left_id", nullable = false)
    private Athlete athleteLeft;

    /** Null when right side is a BYE in elimination bracket */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete_right_id")
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

    @Column(name = "elapsed_seconds", nullable = false)
    private int elapsedSeconds = 0;

    /** True while the referee has the clock stopped (between touches or during card review) */
    @Column(name = "timer_paused", nullable = false)
    private boolean timerPaused = false;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Athlete winner;

    /** Used for ordering bouts within a poule */
    @Column(name = "bout_order")
    private Integer boutOrder;

    /** Piste (strip) where the bout takes place, assigned by the organizer (e.g. "Pista A") */
    @Column(name = "piste", length = 50)
    private String piste;

    /** Non-null for elimination bouts */
    @Enumerated(EnumType.STRING)
    @Column(name = "elimination_round", length = 20)
    private EliminationRound eliminationRound;

    /** Seed position in the elimination bracket (1-based) */
    @Column(name = "bracket_position")
    private Integer bracketPosition;

    /** Priority assigned when scores are tied at end of regulation time.
     *  The fencer with priority wins if no touch is scored in the extra minute. */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 10)
    private EventSide priority;

    /** Referees assigned to this bout */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "bout_referees",
            joinColumns = @JoinColumn(name = "bout_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> referees = new ArrayList<>();

    @OneToMany(mappedBy = "bout", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoutEvent> events = new ArrayList<>();
}
