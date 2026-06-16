package com.touchemanager.athlete.service.impl;

import com.touchemanager.athlete.dto.AthleteRequest;
import com.touchemanager.athlete.dto.AthleteResponse;
import com.touchemanager.athlete.entity.Athlete;
import com.touchemanager.athlete.entity.DominantHand;
import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.athlete.repository.AthleteRepository;
import com.touchemanager.auth.entity.User;
import com.touchemanager.auth.repository.UserRepository;
import com.touchemanager.shared.exception.AthleteAlreadyExistsException;
import com.touchemanager.shared.exception.AthleteNotFoundException;
import com.touchemanager.shared.exception.DniAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AthleteServiceImplTest {

    @Mock
    private AthleteRepository athleteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.touchemanager.bout.repository.BoutRepository boutRepository;

    @Mock
    private com.touchemanager.tournament.repository.EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private AthleteServiceImpl athleteService;

    private User user;
    private Athlete athlete;
    private AthleteRequest athleteRequest;
    private String email;

    @BeforeEach
    void setUp() {
        email = "athlete@test.com";

        user = new User();
        user.setId(1L);
        user.setEmail(email);

        athlete = new Athlete();
        athlete.setId(10L);
        athlete.setUser(user);
        athlete.setFirstName("John");
        athlete.setLastName("Doe");
        athlete.setDni("12345678");
        athlete.setBirthDate(LocalDate.of(1995, 5, 15));
        athlete.setGender(Gender.MALE);
        athlete.setDominantHand(DominantHand.RIGHT);
        athlete.setClub("Fencing Club");
        athlete.setProvince("Buenos Aires");

        athleteRequest = new AthleteRequest(
                "John",
                "Doe",
                "12345678",
                LocalDate.of(1995, 5, 15),
                Gender.MALE,
                DominantHand.RIGHT,
                "Fencing Club",
                "Buenos Aires"
        );
    }

    // --- getProfile Tests ---

    @Test
    void getProfile_Success() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(athleteRepository.findByUserId(user.getId())).thenReturn(Optional.of(athlete));

        AthleteResponse response = athleteService.getProfile(email);

        assertNotNull(response);
        assertEquals(athlete.getId(), response.id());
        assertEquals(user.getId(), response.userId());
        assertEquals(user.getEmail(), response.email());
        assertEquals(athlete.getFirstName(), response.firstName());
        assertEquals(athlete.getLastName(), response.lastName());
        assertEquals(athlete.getDni(), response.dni());
        assertEquals(athlete.getBirthDate(), response.birthDate());
        assertEquals(athlete.getGender(), response.gender());
        assertEquals(athlete.getDominantHand(), response.dominantHand());
        assertEquals(athlete.getClub(), response.club());
        assertEquals(athlete.getProvince(), response.province());

        verify(userRepository, times(1)).findByEmail(email);
        verify(athleteRepository, times(1)).findByUserId(user.getId());
    }

    @Test
    void getProfile_UserNotFound() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> athleteService.getProfile(email));
        assertEquals("User not found: " + email, exception.getMessage());

        verify(userRepository, times(1)).findByEmail(email);
        verifyNoInteractions(athleteRepository);
    }

    @Test
    void getProfile_AthleteNotFound() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(athleteRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        assertThrows(AthleteNotFoundException.class, () -> athleteService.getProfile(email));

        verify(userRepository, times(1)).findByEmail(email);
        verify(athleteRepository, times(1)).findByUserId(user.getId());
    }

    // --- createProfile Tests ---

    @Test
    void createProfile_Success() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(athleteRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(athleteRepository.existsByDni(athleteRequest.getDni())).thenReturn(false);
        when(athleteRepository.save(any(Athlete.class))).thenReturn(athlete);

        AthleteResponse response = athleteService.createProfile(email, athleteRequest);

        assertNotNull(response);
        assertEquals(athlete.getId(), response.id());
        assertEquals(athlete.getFirstName(), response.firstName());

        verify(userRepository, times(1)).findByEmail(email);
        verify(athleteRepository, times(1)).findByUserId(user.getId());
        verify(athleteRepository, times(1)).existsByDni(athleteRequest.getDni());
        verify(athleteRepository, times(1)).save(any(Athlete.class));
    }

    @Test
    void createProfile_UserNotFound() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> athleteService.createProfile(email, athleteRequest));
        assertEquals("User not found: " + email, exception.getMessage());

        verify(userRepository, times(1)).findByEmail(email);
        verifyNoInteractions(athleteRepository);
    }

    @Test
    void createProfile_AthleteAlreadyExists() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(athleteRepository.findByUserId(user.getId())).thenReturn(Optional.of(athlete));

        assertThrows(AthleteAlreadyExistsException.class, () -> athleteService.createProfile(email, athleteRequest));

        verify(userRepository, times(1)).findByEmail(email);
        verify(athleteRepository, times(1)).findByUserId(user.getId());
        verify(athleteRepository, never()).existsByDni(anyString());
        verify(athleteRepository, never()).save(any(Athlete.class));
    }

    @Test
    void createProfile_DniAlreadyExists() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(athleteRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(athleteRepository.existsByDni(athleteRequest.getDni())).thenReturn(true);

        assertThrows(DniAlreadyExistsException.class, () -> athleteService.createProfile(email, athleteRequest));

        verify(userRepository, times(1)).findByEmail(email);
        verify(athleteRepository, times(1)).findByUserId(user.getId());
        verify(athleteRepository, times(1)).existsByDni(athleteRequest.getDni());
        verify(athleteRepository, never()).save(any(Athlete.class));
    }

    // --- updateProfile Tests ---

    @Test
    void updateProfile_Success() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(athleteRepository.findByUserId(user.getId())).thenReturn(Optional.of(athlete));
        when(athleteRepository.findByDni(athleteRequest.getDni())).thenReturn(Optional.of(athlete));
        when(athleteRepository.save(any(Athlete.class))).thenReturn(athlete);

        AthleteResponse response = athleteService.updateProfile(email, athleteRequest);

        assertNotNull(response);
        assertEquals(athlete.getId(), response.id());

        verify(userRepository, times(1)).findByEmail(email);
        verify(athleteRepository, times(1)).findByUserId(user.getId());
        verify(athleteRepository, times(1)).findByDni(athleteRequest.getDni());
        verify(athleteRepository, times(1)).save(any(Athlete.class));
    }

    @Test
    void updateProfile_UserNotFound() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> athleteService.updateProfile(email, athleteRequest));
        assertEquals("User not found: " + email, exception.getMessage());

        verify(userRepository, times(1)).findByEmail(email);
        verifyNoInteractions(athleteRepository);
    }

    @Test
    void updateProfile_AthleteNotFound() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(athleteRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        assertThrows(AthleteNotFoundException.class, () -> athleteService.updateProfile(email, athleteRequest));

        verify(userRepository, times(1)).findByEmail(email);
        verify(athleteRepository, times(1)).findByUserId(user.getId());
        verify(athleteRepository, never()).findByDni(anyString());
        verify(athleteRepository, never()).save(any(Athlete.class));
    }

    @Test
    void updateProfile_DniAlreadyExistsInAnotherAthlete() {
        Athlete otherAthlete = new Athlete();
        otherAthlete.setId(20L); // different ID
        otherAthlete.setDni(athleteRequest.getDni());

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(athleteRepository.findByUserId(user.getId())).thenReturn(Optional.of(athlete));
        when(athleteRepository.findByDni(athleteRequest.getDni())).thenReturn(Optional.of(otherAthlete));

        assertThrows(DniAlreadyExistsException.class, () -> athleteService.updateProfile(email, athleteRequest));

        verify(userRepository, times(1)).findByEmail(email);
        verify(athleteRepository, times(1)).findByUserId(user.getId());
        verify(athleteRepository, times(1)).findByDni(athleteRequest.getDni());
        verify(athleteRepository, never()).save(any(Athlete.class));
    }
}
