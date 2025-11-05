package com.sist.backend.repository;

import com.sist.backend.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Integer> {
    
    /**
     * 특정 문의(Center)에 대한 답변 조회
     */
    List<Answer> findByCenterIdx(Integer centerIdx);
    
    /**
     * 특정 문의(Center)에 대한 활성 답변 조회
     */
    @Query("SELECT a FROM Answer a WHERE a.centerIdx = :centerIdx AND a.status = true")
    Optional<Answer> findActiveByCenterIdx(@Param("centerIdx") Integer centerIdx);
    
    /**
     * 특정 문의(Center)에 대한 답변 존재 여부 확인
     */
    boolean existsByCenterIdxAndStatus(Integer centerIdx, Boolean status);
}

