package com.touchemanager.auth.service.impl;

import com.touchemanager.auth.dto.*;
import com.touchemanager.auth.entity.NombreRol;
import com.touchemanager.auth.entity.Rol;
import com.touchemanager.auth.entity.Usuario;
import com.touchemanager.auth.repository.RolRepository;
import com.touchemanager.auth.repository.UsuarioRepository;
import com.touchemanager.auth.service.JwtService;
import com.touchemanager.auth.service.UsuarioService;
import com.touchemanager.shared.exception.EmailYaExisteException;
import com.touchemanager.shared.exception.InvalidCredentialsException;
import com.touchemanager.shared.exception.RolNoAsignadoException;
import com.touchemanager.shared.exception.RolNoEncontradoException;
import com.touchemanager.shared.exception.UsuarioNoEncontradoException;
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
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public RegisterResponseDTO registrar(RegisterRequestDTO request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new EmailYaExisteException(request.getEmail());
        }

        Set<Rol> roles = resolverRoles(request.getRoles());

        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRoles(roles);

        Usuario guardado = usuarioRepository.save(usuario);

        Set<NombreRol> nombresRoles = guardado.getRoles().stream()
                .map(Rol::getNombre)
                .collect(Collectors.toSet());

        return new RegisterResponseDTO(guardado.getId(), guardado.getEmail(), nombresRoles);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!usuario.isActivo()) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new InvalidCredentialsException();
        }

        Set<NombreRol> roles = usuario.getRoles().stream()
                .map(Rol::getNombre)
                .collect(Collectors.toSet());

        // Single role → emit token directly
        if (roles.size() == 1) {
            NombreRol rol = roles.iterator().next();
            String token = jwtService.generateToken(usuario.getId(), usuario.getEmail(), rol);
            return new LoginResponseDTO(token, null);
        }

        // Multiple roles → return temporary token and role list, frontend must call /select-role
        String tempToken = jwtService.generateToken(usuario.getId(), usuario.getEmail(), null);
        return new LoginResponseDTO(tempToken, roles);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO selectRole(String email, SelectRoleRequestDTO request) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        boolean hasRole = usuario.getRoles().stream()
                .map(Rol::getNombre)
                .anyMatch(r -> r == request.getRol());

        if (!hasRole) {
            throw new RolNoAsignadoException(request.getRol().name());
        }

        String token = jwtService.generateToken(usuario.getId(), usuario.getEmail(), request.getRol());
        return new LoginResponseDTO(token, null);
    }

    private Set<Rol> resolverRoles(Set<NombreRol> nombres) {
        return nombres.stream()
                .map(nombre -> rolRepository.findByNombre(nombre)
                        .orElseThrow(() -> new RolNoEncontradoException(nombre.name())))
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDTO getProfile(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNoEncontradoException(email));

        Set<NombreRol> nombresRoles = usuario.getRoles().stream()
                .map(Rol::getNombre)
                .collect(Collectors.toSet());

        String profilePictureUrl = usuario.getProfilePictureKey() != null
                ? "/api/users/profile-picture/" + usuario.getId()
                : null;

        return new UserProfileDTO(usuario.getId(), usuario.getEmail(), nombresRoles, profilePictureUrl);
    }

    @Override
    @Transactional
    public UserProfileDTO uploadProfilePicture(String email, MultipartFile file) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNoEncontradoException(email));

        // Delete old profile picture if exists
        if (usuario.getProfilePictureKey() != null) {
            try {
                fileStorageService.deleteFile(usuario.getProfilePictureKey());
            } catch (Exception e) {
                // Log and ignore or handle. We'll proceed so database update is clean.
            }
        }

        // Upload new picture
        String fileKey = fileStorageService.uploadFile(file, "profile-pictures/" + usuario.getId());
        usuario.setProfilePictureKey(fileKey);
        usuarioRepository.save(usuario);

        return getProfile(email);
    }

    @Override
    @Transactional
    public UserProfileDTO deleteProfilePicture(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNoEncontradoException(email));

        if (usuario.getProfilePictureKey() != null) {
            fileStorageService.deleteFile(usuario.getProfilePictureKey());
            usuario.setProfilePictureKey(null);
            usuarioRepository.save(usuario);
        }

        return getProfile(email);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream getProfilePicture(Long userId) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new UsuarioNoEncontradoException(String.valueOf(userId)));

        if (usuario.getProfilePictureKey() == null) {
            throw new UsuarioNoEncontradoException("No profile picture set for user " + userId);
        }

        return fileStorageService.downloadFile(usuario.getProfilePictureKey());
    }

    @Override
    @Transactional(readOnly = true)
    public String getProfilePictureContentType(Long userId) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new UsuarioNoEncontradoException(String.valueOf(userId)));

        String key = usuario.getProfilePictureKey();
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
