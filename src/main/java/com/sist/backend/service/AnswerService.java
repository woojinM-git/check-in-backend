package com.sist.backend.service;

import com.sist.backend.entity.Answer;
import com.sist.backend.entity.Center;
import com.sist.backend.repository.AnswerRepository;
import com.sist.backend.repository.CenterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 1대1 문의 답변 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerService {
    
    private final AnswerRepository answerRepository;
    private final CenterRepository centerRepository;
    
    /**
     * 답변 작성
     * @param centerIdx 문의 ID
     * @param adminIdx 관리자 ID
     * @param content 답변 내용
     * @return 생성된 답변
     */
    @Transactional
    public Answer createAnswer(Integer centerIdx, Integer adminIdx, String content) {
        // 1. 문의 존재 확인
        Center center = centerRepository.findById(centerIdx)
            .orElseThrow(() -> new RuntimeException("문의를 찾을 수 없습니다."));
        
        // 2. 이미 답변이 있는지 확인 (활성 답변)
        Optional<Answer> existingAnswer = answerRepository.findActiveByCenterIdx(centerIdx);
        if (existingAnswer.isPresent()) {
            throw new RuntimeException("이미 답변이 작성되어 있습니다.");
        }
        
        // 3. 답변 생성
        Answer answer = new Answer();
        answer.setCenterIdx(centerIdx);
        answer.setContentid(""); // contentid는 빈 문자열로 설정 (필요시 나중에 수정)
        answer.setContent(content.trim());
        answer.setStatus(true); // 활성 상태
        
        Answer savedAnswer = answerRepository.save(answer);
        
        // 4. 문의 상태를 완료(2)로 변경 및 관리자 배정
        center.setStatus(2); // 완료 상태
        center.setAdminIdx(adminIdx); // 답변 작성한 관리자 배정
        
        centerRepository.save(center);
        
        log.info("답변 작성 완료: answerIdx={}, centerIdx={}, adminIdx={}", 
                savedAnswer.getAnswerIdx(), centerIdx, adminIdx);
        
        return savedAnswer;
    }
    
    /**
     * 답변 수정
     * @param answerIdx 답변 ID
     * @param content 수정할 내용
     * @return 수정된 답변
     */
    @Transactional
    public Answer updateAnswer(Integer answerIdx, String content) {
        Answer answer = answerRepository.findById(answerIdx)
            .orElseThrow(() -> new RuntimeException("답변을 찾을 수 없습니다."));
        
        answer.setContent(content.trim());
        
        return answerRepository.save(answer);
    }
    
    /**
     * 답변 조회 (문의 ID로)
     * @param centerIdx 문의 ID
     * @return 답변 목록
     */
    @Transactional(readOnly = true)
    public List<Answer> getAnswersByCenterIdx(Integer centerIdx) {
        return answerRepository.findByCenterIdx(centerIdx);
    }
    
    /**
     * 활성 답변 조회 (문의 ID로)
     * @param centerIdx 문의 ID
     * @return 활성 답변 (없으면 null)
     */
    @Transactional(readOnly = true)
    public Optional<Answer> getActiveAnswerByCenterIdx(Integer centerIdx) {
        return answerRepository.findActiveByCenterIdx(centerIdx);
    }
    
    /**
     * 답변 삭제 (상태 비활성화)
     * @param answerIdx 답변 ID
     */
    @Transactional
    public void deleteAnswer(Integer answerIdx) {
        Answer answer = answerRepository.findById(answerIdx)
            .orElseThrow(() -> new RuntimeException("답변을 찾을 수 없습니다."));
        
        answer.setStatus(false); // 비활성화
        
        answerRepository.save(answer);
        
        // 문의 상태를 처리중(1)으로 변경
        Center center = centerRepository.findById(answer.getCenterIdx())
            .orElseThrow(() -> new RuntimeException("문의를 찾을 수 없습니다."));
        
        center.setStatus(1); // 처리중 상태로 변경
        
        centerRepository.save(center);
    }
}

