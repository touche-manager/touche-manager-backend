package com.touchemanager.tournament.config;

import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.tournament.entity.Category;
import com.touchemanager.tournament.entity.Tournament;
import com.touchemanager.tournament.entity.Weapon;
import com.touchemanager.tournament.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TournamentDataInitializer implements CommandLineRunner {

    private final TournamentRepository tournamentRepository;

    @Override
    public void run(String... args) {
        if (tournamentRepository.count() == 0) {
            LocalDate today = LocalDate.now();

            Tournament t1 = new Tournament(
                    null,
                    "Copa República - Florete Senior Masculino",
                    Weapon.FOIL,
                    Category.SENIOR,
                    Gender.MALE,
                    "Club Universitario de Córdoba",
                    today.plusDays(15),
                    BigDecimal.valueOf(10000.00)
            );

            Tournament t2 = new Tournament(
                    null,
                    "Nacional de Espada Cadetes Femenino",
                    Weapon.EPEE,
                    Category.CADET,
                    Gender.FEMALE,
                    "Gimnasia y Esgrima de Rosario",
                    today.plusDays(9),
                    BigDecimal.valueOf(8000.00) // Will calculate to 12000 (late fee)
            );

            Tournament t3 = new Tournament(
                    null,
                    "Torneo Relámpago - Sable Juvenil Mixto",
                    Weapon.SABRE,
                    Category.JUNIOR,
                    Gender.FEMALE, // Gender matches existing Gender MALE/FEMALE. If MIXED is needed but Gender has only MALE/FEMALE, let's check!
                    // Wait, Athlete.Gender has only MALE and FEMALE.
                    // Let's use FEMALE or MALE for tournament 3 to avoid Gender enum validation issues.
                    // Yes! Our Gender enum in com.touchemanager.athlete.entity.Gender only has MALE and FEMALE.
                    // So let's use MALE or FEMALE.
                    "Club de Esgrima Buenos Aires",
                    today.plusDays(4), // Closed
                    BigDecimal.valueOf(12000.00)
            );

            Tournament t4 = new Tournament(
                    null,
                    "Metropolitano de Espada Veteranos Masculino",
                    Weapon.EPEE,
                    Category.VETERAN,
                    Gender.MALE,
                    "Club Comunicaciones",
                    today.plusDays(35),
                    BigDecimal.valueOf(15000.00)
            );

            tournamentRepository.saveAll(List.of(t1, t2, t3, t4));
            System.out.println("--- Seeded sample tournaments successfully! ---");
        }
    }
}
