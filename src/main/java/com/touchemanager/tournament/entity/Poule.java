package com.touchemanager.tournament.entity;

import com.touchemanager.athlete.entity.Athlete;
import com.touchemanager.auth.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "poules")
@Getter
@Setter
@NoArgsConstructor
public class Poule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(name = "number", nullable = false)
    private int number;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PouleStatus status = PouleStatus.PENDING;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "poule_athletes",
            joinColumns = @JoinColumn(name = "poule_id"),
            inverseJoinColumns = @JoinColumn(name = "athlete_id")
    )
    private List<Athlete> athletes = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "poule_referees",
            joinColumns = @JoinColumn(name = "poule_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> referees = new ArrayList<>();
}
