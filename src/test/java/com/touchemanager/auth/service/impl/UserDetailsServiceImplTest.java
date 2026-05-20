package com.touchemanager.auth.service.impl;

import com.touchemanager.auth.entity.Usuario;
import com.touchemanager.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_Success() {
        String email = "test@test.com";
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPassword("encodedPassword");

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        assertNotNull(userDetails);
        assertEquals(email, userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
        verify(usuarioRepository, times(1)).findByEmail(email);
    }

    @Test
    void loadUserByUsername_UserNotFound() {
        String email = "notfound@test.com";
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(email));
        verify(usuarioRepository, times(1)).findByEmail(email);
    }
}
