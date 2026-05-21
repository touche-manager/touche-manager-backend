package com.touchemanager.auth.service;

import com.touchemanager.auth.dto.*;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

public interface UsuarioService {

    RegisterResponseDTO registrar(RegisterRequestDTO request);

    LoginResponseDTO login(LoginRequestDTO request);

    LoginResponseDTO selectRole(String email, SelectRoleRequestDTO request);

    UserProfileDTO getProfile(String email);

    UserProfileDTO uploadProfilePicture(String email, MultipartFile file);

    UserProfileDTO deleteProfilePicture(String email);

    InputStream getProfilePicture(Long userId);

    String getProfilePictureContentType(Long userId);
}
