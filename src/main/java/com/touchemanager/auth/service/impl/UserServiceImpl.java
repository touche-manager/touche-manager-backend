package com.touchemanager.auth.service.impl;

import com.touchemanager.auth.dto.*;
import com.touchemanager.auth.entity.Role;
import com.touchemanager.auth.entity.RoleName;
import com.touchemanager.auth.entity.User;
import com.touchemanager.auth.repository.RoleRepository;
import com.touchemanager.auth.repository.UserRepository;
import com.touchemanager.auth.service.JwtService;
import com.touchemanager.auth.service.UserService;
import com.touchemanager.shared.exception.EmailAlreadyExistsException;
import com.touchemanager.shared.exception.InvalidCredentialsException;
import com.touchemanager.shared.exception.RoleNotAssignedException;
import com.touchemanager.shared.exception.RoleNotFoundException;
import com.touchemanager.shared.exception.UserNotFoundException;
import com.touchemanager.shared.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public RegisterResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        Set<Role> roles = resolveRoles(request.getRoles());

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(roles);

        User saved = userRepository.save(user);

        Set<RoleName> roleNames = saved.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return new RegisterResponseDTO(saved.getId(), saved.getEmail(), roleNames);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive()) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        Set<RoleName> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        // Single role → emit token directly
        if (roles.size() == 1) {
            RoleName role = roles.iterator().next();
            String token = jwtService.generateToken(user.getId(), user.getEmail(), role);
            return new LoginResponseDTO(token, null);
        }

        // Multiple roles → return temporary token and role list, frontend must call /select-role
        String tempToken = jwtService.generateToken(user.getId(), user.getEmail(), null);
        return new LoginResponseDTO(tempToken, roles);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO selectRole(String email, SelectRoleRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        boolean hasRole = user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(r -> r == request.getRole());

        if (!hasRole) {
            throw new RoleNotAssignedException(request.getRole().name());
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), request.getRole());
        return new LoginResponseDTO(token, null);
    }

    private Set<Role> resolveRoles(Set<RoleName> names) {
        return names.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new RoleNotFoundException(name.name())))
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDTO getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        Set<RoleName> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        String profilePictureUrl = user.getProfilePictureKey() != null
                ? "/api/users/profile-picture/" + user.getId()
                : null;

        return new UserProfileDTO(user.getId(), user.getEmail(), roleNames, profilePictureUrl);
    }

    @Override
    @Transactional
    public UserProfileDTO uploadProfilePicture(String email, MultipartFile file) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        // Delete old profile picture if exists
        if (user.getProfilePictureKey() != null) {
            try {
                fileStorageService.deleteFile(user.getProfilePictureKey());
            } catch (Exception e) {
                // Log and ignore or handle. We'll proceed so database update is clean.
            }
        }

        // Upload new picture
        String fileKey = fileStorageService.uploadFile(file, "profile-pictures/" + user.getId());
        user.setProfilePictureKey(fileKey);
        userRepository.save(user);

        return getProfile(email);
    }

    @Override
    @Transactional
    public UserProfileDTO deleteProfilePicture(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        if (user.getProfilePictureKey() != null) {
            fileStorageService.deleteFile(user.getProfilePictureKey());
            user.setProfilePictureKey(null);
            userRepository.save(user);
        }

        return getProfile(email);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream getProfilePicture(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(String.valueOf(userId)));

        if (user.getProfilePictureKey() == null) {
            throw new UserNotFoundException("No profile picture set for user " + userId);
        }

        return fileStorageService.downloadFile(user.getProfilePictureKey());
    }

    @Override
    @Transactional(readOnly = true)
    public String getProfilePictureContentType(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(String.valueOf(userId)));

        String key = user.getProfilePictureKey();
        if (key == null) {
            return "image/png"; // default fallback
        }

        if (key.toLowerCase().endsWith(".png")) {
            return "image/png";
        } else if (key.toLowerCase().endsWith(".gif")) {
            return "image/gif";
        } else if (key.toLowerCase().endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg"; // default fallback for jpg/jpeg
    }
}
