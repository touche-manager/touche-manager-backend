package com.touchemanager.tournament.entity;

import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.auth.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tournaments")
@Getter
@Setter
@NoArgsConstructor
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "weapon", nullable = false, length = 20)
    private Weapon weapon;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender;

    @Column(name = "location", nullable = false, length = 150)
    private String location;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false, length = 30)
    private TournamentPhase phase = TournamentPhase.ENROLLMENT;

    @Column(name = "advancement_rate", nullable = false, precision = 3, scale = 2)
    private java.math.BigDecimal advancementRate = java.math.BigDecimal.ONE;

    /** Marks this tournament as a National Championship — applies coefficient 1.2 in rankings */
    @Column(name = "is_national", nullable = false)
    private boolean isNational = false;
}
