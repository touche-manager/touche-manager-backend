package com.touchemanager.auth.service.impl;

import com.touchemanager.auth.dto.RegisterRequestDTO;
import com.touchemanager.auth.dto.RegisterResponseDTO;
import com.touchemanager.auth.entity.NombreRol;
import com.touchemanager.auth.entity.Rol;
import com.touchemanager.auth.entity.Usuario;
import com.touchemanager.auth.repository.RolRepository;
import com.touchemanager.auth.repository.UsuarioRepository;
import com.touchemanager.auth.service.UsuarioService;
import com.touchemanager.shared.exception.EmailYaExisteException;
import com.touchemanager.shared.exception.RolNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

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

    private Set<Rol> resolverRoles(Set<NombreRol> nombres) {
        return nombres.stream()
                .map(nombre -> rolRepository.findByNombre(nombre)
                        .orElseThrow(() -> new RolNoEncontradoException(nombre.name())))
                .collect(Collectors.toSet());
    }
}
