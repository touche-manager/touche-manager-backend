package com.touchemanager.bout.repository;

import com.touchemanager.bout.entity.BoutEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoutEventRepository extends JpaRepository<BoutEvent, Long> {

    List<BoutEvent> findByBoutIdOrderByRecordedAtAsc(Long boutId);
}
