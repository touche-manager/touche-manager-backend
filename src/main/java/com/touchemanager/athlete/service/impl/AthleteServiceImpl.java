package com.touchemanager.athlete.service.impl;

import com.touchemanager.athlete.dto.AthleteRequest;
import com.touchemanager.athlete.dto.AthleteResponse;
import com.touchemanager.athlete.entity.Athlete;
import com.touchemanager.auth.entity.User;
import com.touchemanager.athlete.repository.AthleteRepository;
import com.touchemanager.auth.repository.UserRepository;
import com.touchemanager.athlete.service.AthleteService;
import com.touchemanager.shared.exception.AthleteAlreadyExistsException;
import com.touchemanager.shared.exception.AthleteNotFoundException;
import com.touchemanager.shared.exception.DniAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AthleteServiceImpl implements AthleteService {

    private final AthleteRepository athleteRepository;
    private final UserRepository userRepository;

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
