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

// @Component  // Disabled — use the SQL seed script instead
@RequiredArgsConstructor
public class TournamentDataInitializer implements CommandLineRunner {

    private final TournamentRepository tournamentRepository;

    @Override
    public void run(String... args) {
        if (tournamentRepository.count() == 0) {
            LocalDate today = LocalDate.now();

            Tournament t1 = new Tournament();
            t1.setName("Copa República - Florete Senior Masculino");
            t1.setWeapon(Weapon.FOIL);
            t1.setCategory(Category.SENIOR);
            t1.setGender(Gender.MALE);
            t1.setLocation("Club Universitario de Córdoba");
            t1.setDate(today.plusDays(15));
            t1.setBasePrice(BigDecimal.valueOf(10000.00));

            Tournament t2 = new Tournament();
            t2.setName("Nacional de Espada Cadetes Femenino");
            t2.setWeapon(Weapon.EPEE);
            t2.setCategory(Category.CADET);
            t2.setGender(Gender.FEMALE);
            t2.setLocation("Gimnasia y Esgrima de Rosario");
            t2.setDate(today.plusDays(9));
            t2.setBasePrice(BigDecimal.valueOf(8000.00));

            Tournament t3 = new Tournament();
            t3.setName("Torneo Relámpago - Sable Juvenil Femenino");
            t3.setWeapon(Weapon.SABRE);
            t3.setCategory(Category.JUNIOR);
            t3.setGender(Gender.FEMALE);
            t3.setLocation("Club de Esgrima Buenos Aires");
            t3.setDate(today.plusDays(4));
            t3.setBasePrice(BigDecimal.valueOf(12000.00));

            Tournament t4 = new Tournament();
            t4.setName("Metropolitano de Espada Veteranos Masculino");
            t4.setWeapon(Weapon.EPEE);
            t4.setCategory(Category.VETERAN);
            t4.setGender(Gender.MALE);
            t4.setLocation("Club Comunicaciones");
            t4.setDate(today.plusDays(35));
            t4.setBasePrice(BigDecimal.valueOf(15000.00));

            tournamentRepository.saveAll(List.of(t1, t2, t3, t4));
            System.out.println("--- Seeded sample tournaments successfully! ---");
        }
    }
}
