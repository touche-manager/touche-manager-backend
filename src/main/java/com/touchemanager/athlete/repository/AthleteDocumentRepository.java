package com.touchemanager.athlete.repository;

import com.touchemanager.athlete.entity.AthleteDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AthleteDocumentRepository extends JpaRepository<AthleteDocument, Long> {
    List<AthleteDocument> findByAthleteId(Long athleteId);
}
