package com.touchemanager.athlete.service.impl;

import com.touchemanager.athlete.dto.AthleteBoutResponse;
import com.touchemanager.athlete.dto.AthleteRequest;
import com.touchemanager.athlete.dto.AthleteResponse;
import com.touchemanager.athlete.entity.Athlete;
import com.touchemanager.auth.entity.User;
import com.touchemanager.athlete.repository.AthleteRepository;
import com.touchemanager.auth.repository.UserRepository;
import com.touchemanager.athlete.service.AthleteService;
import com.touchemanager.bout.entity.Bout;
import com.touchemanager.bout.entity.BoutStatus;
import com.touchemanager.bout.repository.BoutRepository;
import com.touchemanager.shared.exception.AthleteAlreadyExistsException;
import com.touchemanager.shared.exception.AthleteNotFoundException;
import com.touchemanager.shared.exception.DniAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AthleteServiceImpl implements AthleteService {

    private final AthleteRepository athleteRepository;
    private final UserRepository userRepository;
    private final BoutRepository boutRepository;

    @Override
    @Transactional(readOnly = true)
    public AthleteResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        Athlete athlete = athleteRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AthleteNotFoundException(email));

        return mapToResponse(athlete);
    }

    @Override
    @Transactional
    public AthleteResponse createProfile(String email, AthleteRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        if (athleteRepository.findByUserId(user.getId()).isPresent()) {
            throw new AthleteAlreadyExistsException(email);
        }

        if (athleteRepository.existsByDni(request.getDni())) {
            throw new DniAlreadyExistsException(request.getDni());
        }

        Athlete athlete = new Athlete();
        athlete.setUser(user);
        athlete.setFirstName(request.getFirstName());
        athlete.setLastName(request.getLastName());
        athlete.setDni(request.getDni());
        athlete.setBirthDate(request.getBirthDate());
        athlete.setGender(request.getGender());
        athlete.setDominantHand(request.getDominantHand());
        athlete.setClub(request.getClub());
        athlete.setProvince(request.getProvince());

        Athlete saved = athleteRepository.save(athlete);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public AthleteResponse updateProfile(String email, AthleteRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        Athlete athlete = athleteRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AthleteNotFoundException(email));

        athleteRepository.findByDni(request.getDni()).ifPresent(other -> {
            if (!other.getId().equals(athlete.getId())) {
                throw new DniAlreadyExistsException(request.getDni());
            }
        });

        athlete.setFirstName(request.getFirstName());
        athlete.setLastName(request.getLastName());
        athlete.setDni(request.getDni());
        athlete.setBirthDate(request.getBirthDate());
        athlete.setGender(request.getGender());
        athlete.setDominantHand(request.getDominantHand());
        athlete.setClub(request.getClub());
        athlete.setProvince(request.getProvince());

        Athlete saved = athleteRepository.save(athlete);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AthleteBoutResponse> getMyBouts(String email, Long tournamentId, BoutStatus status) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        Athlete athlete = athleteRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AthleteNotFoundException(email));

        return boutRepository.findByAthleteId(athlete.getId()).stream()
                .filter(b -> tournamentId == null || b.getTournament().getId().equals(tournamentId))
                .filter(b -> status == null || b.getStatus() == status)
                .map(b -> toAthleteBoutResponse(b, athlete.getId()))
                .toList();
    }

    private AthleteBoutResponse toAthleteBoutResponse(Bout bout, Long myAthleteId) {
        boolean iAmLeft = bout.getAthleteLeft().getId().equals(myAthleteId);
        Athlete opponent = iAmLeft ? bout.getAthleteRight() : bout.getAthleteLeft();

        Boolean won = null;
        if (bout.getStatus() == BoutStatus.FINISHED && bout.getWinner() != null) {
            won = bout.getWinner().getId().equals(myAthleteId);
        }

        return new AthleteBoutResponse(
                bout.getId(),
                bout.getTournament().getId(),
                bout.getTournament().getName(),
                bout.getTournament().getDate(),
                bout.getFormat(),
                bout.getEliminationRound(),
                bout.getPoule() != null ? bout.getPoule().getNumber() : null,
                opponent != null ? opponent.getFirstName() + " " + opponent.getLastName() : "BYE",
                opponent != null ? opponent.getClub() : null,
                iAmLeft ? bout.getScoreLeft() : bout.getScoreRight(),
                iAmLeft ? bout.getScoreRight() : bout.getScoreLeft(),
                won,
                bout.getStatus(),
                bout.getPiste(),
                bout.getFinishedAt()
        );
    }

    private AthleteResponse mapToResponse(Athlete athlete) {
        return new AthleteResponse(
                athlete.getId(),
                athlete.getUser().getId(),
                athlete.getUser().getEmail(),
                athlete.getFirstName(),
                athlete.getLastName(),
                athlete.getDni(),
                athlete.getBirthDate(),
                athlete.getGender(),
                athlete.getDominantHand(),
                athlete.getClub(),
                athlete.getProvince()
        );
    }
}
