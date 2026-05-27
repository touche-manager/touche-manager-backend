package com.touchemanager.auth.service.impl;

import com.touchemanager.auth.dto.*;
import com.touchemanager.auth.entity.Role;
import com.touchemanager.auth.entity.RoleName;
import com.touchemanager.auth.entity.User;
import com.touchemanager.auth.repository.RoleRepository;
import com.touchemanager.auth.repository.UserRepository;
import com.touchemanager.auth.service.JwtService;
import com.touchemanager.shared.exception.EmailAlreadyExistsException;
import com.touchemanager.shared.exception.InvalidCredentialsException;
import com.touchemanager.shared.exception.RoleNotAssignedException;
import com.touchemanager.shared.exception.RoleNotFoundException;
import com.touchemanager.shared.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private UserServiceImpl userService;

    private RegisterRequestDTO request;
    private Role roleAthlete;
    private Role roleOrganizer;
    private User user;

    @BeforeEach
    void setUp() {
        request = new RegisterRequestDTO();
        request.setEmail("test@test.com");
        request.setPassword("password123");
        request.setRoles(Set.of(RoleName.ATHLETE));

        roleAthlete = new Role();
        roleAthlete.setId(1L);
        roleAthlete.setName(RoleName.ATHLETE);

        roleOrganizer = new Role();
        roleOrganizer.setId(2L);
        roleOrganizer.setName(RoleName.ORGANIZER);

        user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setPassword("encodedPassword");
        user.setActive(true);
        user.setRoles(Set.of(roleAthlete));
    }

    @Test
    void register_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ATHLETE)).thenReturn(Optional.of(roleAthlete));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        RegisterResponseDTO response = userService.register(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("test@test.com", response.email());
        assertTrue(response.roles().contains(RoleName.ATHLETE));

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_EmailAlreadyExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        assertThrows(EmailAlreadyExistsException.class, () -> userService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_RoleNotFound() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ATHLETE)).thenReturn(Optional.empty());
        assertThrows(RoleNotFoundException.class, () -> userService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_SuccessSingleRole() {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("password123");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken(1L, "test@test.com", RoleName.ATHLETE)).thenReturn("mockedToken");

        LoginResponseDTO response = userService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mockedToken", response.token());
        assertNull(response.roles());
    }

    @Test
    void login_SuccessMultipleRoles() {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("password123");

        user.setRoles(Set.of(roleAthlete, roleOrganizer));

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        LoginResponseDTO response = userService.login(loginRequest);

        assertNotNull(response);
        assertNull(response.token());
        assertNotNull(response.roles());
        assertEquals(2, response.roles().size());
        assertTrue(response.roles().contains(RoleName.ATHLETE));
        assertTrue(response.roles().contains(RoleName.ORGANIZER));
    }

    @Test
    void login_InvalidCredentials() {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("wrongPassword");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> userService.login(loginRequest));
    }

    @Test
    void login_UserInactive() {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("password123");

        user.setActive(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> userService.login(loginRequest));
    }

    @Test
    void selectRole_Success() {
        SelectRoleRequestDTO selectRoleRequest = new SelectRoleRequestDTO();
        selectRoleRequest.setRole(RoleName.ATHLETE);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(1L, "test@test.com", RoleName.ATHLETE)).thenReturn("mockedToken");

        LoginResponseDTO response = userService.selectRole("test@test.com", selectRoleRequest);

        assertNotNull(response);
        assertEquals("mockedToken", response.token());
        assertNull(response.roles());
    }

    @Test
    void selectRole_RoleNotAssigned() {
        SelectRoleRequestDTO selectRoleRequest = new SelectRoleRequestDTO();
        selectRoleRequest.setRole(RoleName.ORGANIZER);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        assertThrows(RoleNotAssignedException.class, () -> userService.selectRole("test@test.com", selectRoleRequest));
    }

    @Test
    void getProfile_Success() {
        user.setProfilePictureKey("profile-pictures/1/pic.png");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        UserProfileDTO profile = userService.getProfile("test@test.com");

        assertNotNull(profile);
        assertEquals(user.getId(), profile.id());
        assertEquals(user.getEmail(), profile.email());
        assertEquals("/api/users/profile-picture/1", profile.profilePictureUrl());
    }

    @Test
    void uploadProfilePicture_Success() {
        MockMultipartFile file = new MockMultipartFile("file", "pic.png", "image/png", "data".getBytes());
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(fileStorageService.uploadFile(any(), anyString())).thenReturn("profile-pictures/1/new_pic.png");

        UserProfileDTO profile = userService.uploadProfilePicture("test@test.com", file);

        assertNotNull(profile);
        assertEquals("/api/users/profile-picture/1", profile.profilePictureUrl());
        verify(fileStorageService, times(1)).uploadFile(file, "profile-pictures/1");
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void deleteProfilePicture_Success() {
        user.setProfilePictureKey("profile-pictures/1/pic.png");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        UserProfileDTO profile = userService.deleteProfilePicture("test@test.com");

        assertNotNull(profile);
        assertNull(profile.profilePictureUrl());
        verify(fileStorageService, times(1)).deleteFile("profile-pictures/1/pic.png");
        assertNull(user.getProfilePictureKey());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void getProfilePicture_Success() {
        user.setProfilePictureKey("profile-pictures/1/pic.png");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(fileStorageService.downloadFile("profile-pictures/1/pic.png")).thenReturn(new ByteArrayInputStream("data".getBytes()));

        InputStream stream = userService.getProfilePicture(1L);

        assertNotNull(stream);
        verify(fileStorageService, times(1)).downloadFile("profile-pictures/1/pic.png");
    }

    @Test
    void getProfilePictureContentType_Success() {
        user.setProfilePictureKey("profile-pictures/1/pic.png");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        String contentType = userService.getProfilePictureContentType(1L);

        assertEquals("image/png", contentType);
    }
}
