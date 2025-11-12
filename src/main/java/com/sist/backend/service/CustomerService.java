package com.sist.backend.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sist.backend.dto.admin.CustomerListDto;
import com.sist.backend.dto.master.CustomerDto;
import com.sist.backend.entity.Customer;
import com.sist.backend.repository.CustomerRepository;
import com.sist.backend.repository.RoomPaymentRepository;
import com.sist.backend.repository.RoomReservationRepository;

import lombok.RequiredArgsConstructor;

import static com.sist.backend.entity.QCustomer.customer;

@Service
@RequiredArgsConstructor
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    private final RoomReservationRepository roomReservationRepository;
    private final RoomPaymentRepository roomPaymentRepository;
    private final JPAQueryFactory queryFactory;

    public int findRegistrationCustomerCount(){
        return customerRepository.findRegistrationCustomerCount();
    }
    public Optional<Customer> findById(String id){
        return customerRepository.findById(id);
    }

    public Optional<Customer> findByIdAndStatus(String id, Integer status){
        return customerRepository.findByIdAndStatus(id, status);
    }
    public Optional<Customer> findByNicknameAndStatus(String nickname, Integer status){
        return customerRepository.findByNicknameAndStatus(nickname, status);
    }

    public Optional<Customer> findByRefTokenAndId(String tokenID, String id){
        return customerRepository.findByRefTokenAndId(tokenID, id);
    }

    public Optional<Customer> findByCustomerIdx(Integer customerIdx){
        return customerRepository.findByCustomerIdx(customerIdx);
    }
    public Optional<Customer> findByIdAndEmailAndName(String id, String email, String name) {
        return customerRepository.findByIdAndEmailAndName(id, email, name);
    }
        
    /* 대시보드의 최근 가입한 고객 목록 (상위 5개) */
    public List<Customer> findByJoinDate(){
        return customerRepository.findByJoinDate();
    }

    public Page<CustomerDto> findCustomerAndRankDto(Pageable pageable){
        Page<Customer> customerPage = customerRepository.findCustomerAndRank(pageable);
        return customerPage.map(customer -> {
            CustomerDto dto = CustomerDto.fromEntity(customer);
            Long reservationCount = roomReservationRepository.countByCustomerIdxAndStatus(customer.getCustomerIdx(), 4);
            dto.setReservationCount(reservationCount);
            return dto;
        });
    }

    public Customer save(Customer customer){
        return customerRepository.save(customer);
    }

    public List<Customer> findAll(){
        return customerRepository.findAll();
    }
    
    // 닉네임으로 고객 검색
    public List<Customer> findByNicknameContaining(String nickname) {
        return customerRepository.findByNicknameContaining(nickname);
    }

    // 해당 호텔을 이용한 고객 중에서 검색어로 필터링
    public List<Customer> findByContentIdAndSearchTerm(String contentId, String searchTerm) {
        return customerRepository.findByContentIdAndSearchTerm(contentId, searchTerm);
    }

    public Optional<Customer> findByEmail(String email) {
        return customerRepository.findByEmail(email);
    }
    public Optional<Customer> findByEmailAndStatus(String email, Integer status) {
        return customerRepository.findByEmailAndStatus(email, status);
    }
    public Optional<Customer> findByCustomerIdxAndStatus(Integer customerIdx, Integer status) {
        return customerRepository.findByCustomerIdxAndStatus(customerIdx, status);
    }

    /* 회원 정지 처리 (status를 1로 변경) */
    @Transactional
    public Customer suspendCustomer(Integer customerIdx) {
        Customer customer = customerRepository.findByCustomerIdx(customerIdx)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        
        customer.setStatus(1);
        return customerRepository.save(customer);
    }

    /* 회원 활성화 처리 (status를 0으로 변경) */
    @Transactional
    public Customer activateCustomer(Integer customerIdx) {
        Customer customer = customerRepository.findByCustomerIdx(customerIdx)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        
        customer.setStatus(0);
        return customerRepository.save(customer);
    }

    /* 회원 비활성화 처리 (status를 1로 변경) */
    @Transactional
    public Customer deactivateCustomer(Integer customerIdx) {
        Customer customer = customerRepository.findByCustomerIdx(customerIdx)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        
        customer.setStatus(1);
        return customerRepository.save(customer);
    }

    /* 회원 일괄 처리 */
    @Transactional
    public int batchUpdateCustomerStatus(List<Integer> customerIdxList, Integer status) {
        if (customerIdxList == null || customerIdxList.isEmpty()) {
            throw new IllegalArgumentException("회원 목록이 비어있습니다.");
        }

        int successCount = 0;
        for (Integer customerIdx : customerIdxList) {
            try {
                Customer customer = customerRepository.findByCustomerIdx(customerIdx)
                    .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + customerIdx));
                
                customer.setStatus(status);
                customerRepository.save(customer);
                successCount++;
            } catch (Exception e) {
                // 개별 회원 처리 실패 시 로그만 남기고 계속 진행
                System.err.println("회원 처리 실패 (customerIdx: " + customerIdx + "): " + e.getMessage());
            }
        }

        return successCount;
    }

    /**
     * 회원 검색 및 필터링 (QueryDSL 사용)
     * 회원명, 이메일, 전화번호로 검색하고 상태로 필터링
     * 
     * @param searchTerm 검색어 (회원명, 이메일, 전화번호)
     * @param statusFilter 상태 필터 ("all", "active", "inactive", "suspended")
     * @param pageable 페이지 정보
     * @return 검색된 회원 목록 (Page)
     */
    public Page<CustomerDto> searchCustomers(String searchTerm, String statusFilter, Pageable pageable) {
        // QueryDSL 조건 빌더
        com.querydsl.core.BooleanBuilder builder = new com.querydsl.core.BooleanBuilder();

        // 검색어 조건 (회원명, 이메일, 전화번호)
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String searchPattern = "%" + searchTerm.trim() + "%";
            builder.and(
                customer.name.likeIgnoreCase(searchPattern)
                    .or(customer.email.likeIgnoreCase(searchPattern))
                    .or(customer.phone.likeIgnoreCase(searchPattern))
            );
        }

        // 상태 필터 조건
        if (statusFilter != null && !statusFilter.equals("all")) {
            switch (statusFilter) {
                case "active":
                    builder.and(customer.status.eq(0));
                    break;
                case "inactive":
                case "suspended":
                    builder.and(customer.status.eq(1));
                    break;
            }
        }

        // QueryDSL 쿼리 실행
        List<Customer> customerList = queryFactory
            .selectFrom(customer)
            .where(builder)
            .orderBy(customer.customerIdx.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        // 전체 개수 조회
        Long total = queryFactory
            .select(customer.count())
            .from(customer)
            .where(builder)
            .fetchOne();

        // CustomerDto로 변환 (예약 건수 포함)
        List<CustomerDto> customerDtoList = customerList.stream()
            .map(c -> {
                CustomerDto dto = CustomerDto.fromEntity(c);
                Long reservationCount = roomReservationRepository.countByCustomerIdxAndStatus(c.getCustomerIdx(), 4);
                dto.setReservationCount(reservationCount);
                return dto;
            })
            .collect(Collectors.toList());

        // Page 객체 생성
        return new org.springframework.data.domain.PageImpl<>(
            customerDtoList,
            pageable,
            total != null ? total : 0L
        );
    }

    /* 특정 호텔을 이용한 고객 목록 조회 (예약 통계 포함) */
    public List<CustomerListDto> findCustomersByContentId(String contentId) {
        // 특정 호텔을 이용한 고객들의 customerIdx 목록 조회
        List<Integer> customerIdxList = roomReservationRepository.findByStatus(contentId)
            .stream()
            .map(r -> r.getCustomerIdx())
            .distinct()
            .collect(Collectors.toList());

        return customerIdxList.stream()
            .map(customerIdx -> {
                Optional<Customer> customerOpt = customerRepository.findByCustomerIdx(customerIdx);
                if (customerOpt.isEmpty()) {
                    return null;
                }
                Customer customer = customerOpt.get();

                // 예약 횟수
                long reservationCount = roomReservationRepository.findByCustomerIdxAndStatus(customerIdx, 1)
                    .stream()
                    .filter(r -> r.getContentid().equals(contentId))
                    .count();

                // 총 결제 금액 (RoomPayment에서 가져오기)
                Long totalPaymentAmount = roomPaymentRepository.findAllByContentIdWithReservations(contentId)
                    .stream()
                    .filter(rp -> rp.getCustomerIdx().equals(customerIdx) && rp.getStatus() == 1)
                    .mapToLong(rp -> rp.getPrice() != null ? rp.getPrice() : 0L)
                    .sum();

                // 최근 방문 날짜
                java.time.LocalDate lastVisitDate = roomReservationRepository
                    .findLastVisitDateByCustomerAndContentId(customerIdx, contentId);

                // 체크인 날짜 목록
                List<java.time.LocalDate> visitedDates = roomReservationRepository
                    .findCheckinDatesByCustomerAndContentId(customerIdx, contentId);

                CustomerListDto dto = new CustomerListDto();
                dto.setCustomerIdx(customer.getCustomerIdx());
                dto.setId(customer.getId());
                dto.setName(customer.getName());
                dto.setEmail(customer.getEmail());
                dto.setPhone(customer.getPhone());
                dto.setReservationCount(reservationCount);
                dto.setTotalPaymentAmount(totalPaymentAmount);
                dto.setLastVisitDate(lastVisitDate);
                dto.setRank(customer.getRank());
                dto.setVisitedDates(visitedDates);

                return dto;
            })
            .filter(dto -> dto != null)
            .collect(Collectors.toList());
    }
}
