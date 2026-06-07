package com.touchemanager.tournament.service.impl;

import com.touchemanager.athlete.repository.AthleteDocumentRepository;
import com.touchemanager.auth.entity.Role;
import com.touchemanager.auth.entity.RoleName;
import com.touchemanager.auth.entity.User;
import com.touchemanager.auth.repository.UserRepository;
import com.touchemanager.shared.exception.TournamentNotOwnedException;
import com.touchemanager.tournament.dto.OrganizerTournamentResponse;
import com.touchemanager.tournament.dto.TournamentRequest;
import com.touchemanager.tournament.entity.Category;
import com.touchemanager.tournament.entity.Tournament;
import com.touchemanager.tournament.entity.Weapon;
import com.touchemanager.tournament.repository.EnrollmentRepository;
import com.touchemanager.tournament.repository.TournamentRepository;
import com.touchemanager.athlete.entity.Gender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizerTournamentServiceImplTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private AthleteDocumentRepository athleteDocumentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrganizerTournamentServiceImpl service;

    private User organizer;
    private TournamentRequest request;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setName(RoleName.ORGANIZER);

        organizer = new User();
        organizer.setId(1L);
        organizer.setEmail("org@test.com");
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        organizer.setRoles(roles);

        request = new TournamentRequest();
        request.setName("Open Nacional");
        request.setWeapon(Weapon.FOIL);
        request.setCategory(Category.SENIOR);
        request.setGender(Gender.MALE);
        request.setLocation("Buenos Aires");
        request.setDate(LocalDate.now().plusMonths(2));
        request.setBasePrice(BigDecimal.valueOf(5000));
    }

    @Test
    void createTournament_Success() {
        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));

        Tournament savedTournament = new Tournament();
        savedTournament.setId(10L);
        savedTournament.setName("Open Nacional");
        savedTournament.setWeapon(Weapon.FOIL);
        savedTournament.setCategory(Category.SENIOR);
        savedTournament.setGender(Gender.MALE);
        savedTournament.setLocation("Buenos Aires");
        savedTournament.setDate(LocalDate.now().plusMonths(2));
        savedTournament.setBasePrice(BigDecimal.valueOf(5000));
        savedTournament.setCreatedBy(organizer);

        when(tournamentRepository.save(any(Tournament.class))).thenReturn(savedTournament);


        OrganizerTournamentResponse response = service.createTournament("org@test.com", request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Open Nacional");
        assertThat(response.totalEnrollments()).isEqualTo(0);
    }

    @Test
    void updateTournament_ThrowsWhenNotOwner() {
        User otherOrganizer = new User();
        otherOrganizer.setId(99L);
        otherOrganizer.setEmail("other@test.com");
        Role role = new Role();
        role.setName(RoleName.ORGANIZER);
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        otherOrganizer.setRoles(roles);

        Tournament tournament = new Tournament();
        tournament.setId(5L);
        tournament.setCreatedBy(organizer); // owned by organizer, not otherOrganizer

        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherOrganizer));
        when(tournamentRepository.findById(5L)).thenReturn(Optional.of(tournament));

        assertThatThrownBy(() -> service.updateTournament("other@test.com", 5L, request))
                .isInstanceOf(TournamentNotOwnedException.class);
    }
}
