package com.sist.backend.repository;

import com.sist.backend.entity.PointLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointLedgerRepository extends JpaRepository<PointLedger, Long> {
}

