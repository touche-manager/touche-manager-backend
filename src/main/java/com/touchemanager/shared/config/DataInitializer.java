package com.touchemanager.shared.config;

import com.touchemanager.auth.entity.NombreRol;
import com.touchemanager.auth.entity.Rol;
import com.touchemanager.auth.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;

    @Override
    public void run(String... args) {
        Arrays.stream(NombreRol.values())
                .filter(nombre -> rolRepository.findByNombre(nombre).isEmpty())
                .forEach(nombre -> rolRepository.save(new Rol(nombre)));
    }
}
