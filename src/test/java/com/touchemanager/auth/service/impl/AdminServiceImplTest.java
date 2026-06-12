package com.touchemanager.auth.service.impl;

import com.touchemanager.athlete.entity.DocumentValidationStatus;
import com.touchemanager.athlete.repository.AthleteDocumentRepository;
import com.touchemanager.athlete.repository.AthleteRepository;
import com.touchemanager.auth.dto.AdminStatsResponse;
import com.touchemanager.auth.dto.AdminUserResponse;
import com.touchemanager.auth.dto.UpdateUserRoleRequest;
import com.touchemanager.auth.entity.Role;
import com.touchemanager.auth.entity.RoleName;
import com.touchemanager.auth.entity.User;
import com.touchemanager.auth.repository.RoleRepository;
import com.touchemanager.auth.repository.UserRepository;
import com.touchemanager.shared.exception.UserNotFoundException;
import com.touchemanager.tournament.entity.TournamentPhase;
import com.touchemanager.tournament.repository.EnrollmentRepository;
import com.touchemanager.tournament.repository.TournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AthleteRepository athleteRepository;

    @Mock
    private AthleteDocumentRepository athleteDocumentRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private AdminServiceImpl service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        ReflectionTestUtils.setField(user, "id", 1L);
        user.setEmail("athlete@test.com");
        user.getRoles().add(new Role(RoleName.ATHLETE));
    }

    private UpdateUserRoleRequest request(RoleName role, UpdateUserRoleRequest.Action action) {
        UpdateUserRoleRequest r = new UpdateUserRoleRequest();
        r.setRole(role);
        r.setAction(action);
        return r;
    }

    @Test
    @DisplayName("updateUserRole ADD grants a new role")
    void updateUserRole_addsRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.ORGANIZER)).thenReturn(Optional.of(new Role(RoleName.ORGANIZER)));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserResponse result = service.updateUserRole(1L, request(RoleName.ORGANIZER, UpdateUserRoleRequest.Action.ADD));

        assertThat(result.roles()).containsExactlyInAnyOrder("ATHLETE", "ORGANIZER");
    }

    @Test
    @DisplayName("updateUserRole REMOVE revokes an existing role")
    void updateUserRole_removesRole() {
        user.getRoles().add(new Role(RoleName.REFEREE));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.REFEREE)).thenReturn(Optional.of(new Role(RoleName.REFEREE)));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserResponse result = service.updateUserRole(1L, request(RoleName.REFEREE, UpdateUserRoleRequest.Action.REMOVE));

        assertThat(result.roles()).containsExactly("ATHLETE");
    }

    @Test
    @DisplayName("updateUserRole REMOVE refuses to leave a user without roles")
    void updateUserRole_cannotRemoveLastRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.ATHLETE)).thenReturn(Optional.of(new Role(RoleName.ATHLETE)));

        assertThatThrownBy(() ->
                service.updateUserRole(1L, request(RoleName.ATHLETE, UpdateUserRoleRequest.Action.REMOVE)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("updateUserRole fails for an unknown user")
    void updateUserRole_unknownUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.updateUserRole(99L, request(RoleName.ORGANIZER, UpdateUserRoleRequest.Action.ADD)))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("getStats aggregates the system counters")
    void getStats_aggregatesCounters() {
        when(userRepository.count()).thenReturn(20L);
        when(athleteRepository.count()).thenReturn(12L);
        when(tournamentRepository.count()).thenReturn(5L);
        when(tournamentRepository.countByPhase(TournamentPhase.FINISHED)).thenReturn(3L);
        when(enrollmentRepository.count()).thenReturn(40L);
        when(athleteDocumentRepository.countByValidationStatus(DocumentValidationStatus.PENDING)).thenReturn(7L);

        AdminStatsResponse stats = service.getStats();

        assertThat(stats.totalUsers()).isEqualTo(20);
        assertThat(stats.totalAthletes()).isEqualTo(12);
        assertThat(stats.totalTournaments()).isEqualTo(5);
        assertThat(stats.activeTournaments()).isEqualTo(2);
        assertThat(stats.finishedTournaments()).isEqualTo(3);
        assertThat(stats.totalEnrollments()).isEqualTo(40);
        assertThat(stats.pendingDocuments()).isEqualTo(7);
    }
}
