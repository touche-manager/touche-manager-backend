package com.touchemanager.bout.service.impl;

import com.touchemanager.athlete.entity.Athlete;
import com.touchemanager.athlete.repository.AthleteRepository;
import com.touchemanager.bout.dto.BoutEventRequest;
import com.touchemanager.bout.dto.BoutRequest;
import com.touchemanager.bout.dto.BoutResponse;
import com.touchemanager.bout.entity.Bout;
import com.touchemanager.bout.entity.BoutEvent;
import com.touchemanager.bout.entity.BoutFormat;
import com.touchemanager.bout.entity.BoutStatus;
import com.touchemanager.bout.entity.EventSide;
import com.touchemanager.bout.entity.EventType;
import com.touchemanager.bout.repository.BoutEventRepository;
import com.touchemanager.bout.repository.BoutRepository;
import com.touchemanager.tournament.entity.Category;
import com.touchemanager.tournament.entity.Tournament;
import com.touchemanager.tournament.entity.Weapon;
import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.tournament.repository.TournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoutServiceImplTest {

    @Mock
    private BoutRepository boutRepository;

    @Mock
    private BoutEventRepository boutEventRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private AthleteRepository athleteRepository;

    @InjectMocks
    private BoutServiceImpl service;

    private Tournament tournament;
    private Athlete athleteLeft;
    private Athlete athleteRight;
    private Bout activeBout;

    @BeforeEach
    void setUp() {
        tournament = new Tournament();
        tournament.setId(1L);
        tournament.setName("Test Tournament");
        tournament.setWeapon(Weapon.FOIL);
        tournament.setCategory(Category.SENIOR);
        tournament.setGender(Gender.MALE);
        tournament.setLocation("Test");
        tournament.setDate(LocalDate.now().plusMonths(1));
        tournament.setBasePrice(BigDecimal.valueOf(1000));

        athleteLeft = new Athlete();
        athleteLeft.setId(10L);
        athleteLeft.setFirstName("Ana");
        athleteLeft.setLastName("García");

        athleteRight = new Athlete();
        athleteRight.setId(20L);
        athleteRight.setFirstName("Pedro");
        athleteRight.setLastName("López");

        activeBout = new Bout();
        activeBout.setId(100L);
        activeBout.setTournament(tournament);
        activeBout.setAthleteLeft(athleteLeft);
        activeBout.setAthleteRight(athleteRight);
        activeBout.setFormat(BoutFormat.POULE);
        activeBout.setStatus(BoutStatus.IN_PROGRESS);
        activeBout.setEvents(new ArrayList<>());
    }

    @Test
    void createBout_Success() {
        BoutRequest request = new BoutRequest();
        request.setTournamentId(1L);
        request.setAthleteLeftId(10L);
        request.setAthleteRightId(20L);
        request.setFormat(BoutFormat.POULE);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(athleteRepository.findById(10L)).thenReturn(Optional.of(athleteLeft));
        when(athleteRepository.findById(20L)).thenReturn(Optional.of(athleteRight));

        Bout savedBout = new Bout();
        savedBout.setId(100L);
        savedBout.setTournament(tournament);
        savedBout.setAthleteLeft(athleteLeft);
        savedBout.setAthleteRight(athleteRight);
        savedBout.setFormat(BoutFormat.POULE);
        savedBout.setStatus(BoutStatus.PENDING);
        savedBout.setEvents(new ArrayList<>());

        when(boutRepository.save(any(Bout.class))).thenReturn(savedBout);

        BoutResponse response = service.createBout("referee@test.com", request);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(BoutStatus.PENDING);
        assertThat(response.touchesTarget()).isEqualTo(5); // POULE format
        assertThat(response.maxPeriods()).isEqualTo(1);
    }

    @Test
    void recordEvent_AutoFinishesPouleAtTarget() {
        // Bout already has 4 points on the left — next touche should finish it
        activeBout.setScoreLeft(4);

        BoutEvent mockEvent = new BoutEvent();
        mockEvent.setId(1L);
        mockEvent.setSide(EventSide.LEFT);
        mockEvent.setEventType(EventType.TOUCHE);
        mockEvent.setScoreDelta(1);

        when(boutRepository.findById(100L)).thenReturn(Optional.of(activeBout));
        when(boutEventRepository.save(any(BoutEvent.class))).thenReturn(mockEvent);
        when(boutRepository.save(any(Bout.class))).thenAnswer(inv -> inv.getArgument(0));

        BoutEventRequest eventRequest = new BoutEventRequest();
        eventRequest.setSide(EventSide.LEFT);
        eventRequest.setEventType(EventType.TOUCHE);

        BoutResponse response = service.recordEvent("referee@test.com", 100L, eventRequest);

        assertThat(response.scoreLeft()).isEqualTo(5);
        assertThat(response.status()).isEqualTo(BoutStatus.FINISHED);
        assertThat(response.winnerId()).isEqualTo(athleteLeft.getId());
    }
}
