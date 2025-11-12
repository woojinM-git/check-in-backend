package com.sist.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    
    @Query("SELECT COUNT(c) FROM Customer c " +
        "WHERE c.status = 0")
    int findRegistrationCustomerCount();

    @Query(value = "SELECT * FROM customer WHERE joinDate >= CURDATE() AND joinDate < DATE_ADD(CURDATE(), INTERVAL 1 DAY) ORDER BY joinDate DESC LIMIT 5", nativeQuery = true)
    List<Customer> findByJoinDate();

    Optional<Customer> findById(String id);

    Optional<Customer> findByIdAndStatus(String id, Integer status);

    Optional<Customer> findByNicknameAndStatus(String nickname, Integer status);

    Optional<Customer> findByCustomerIdx(Integer customerIdx);

    Optional<Customer> findByIdAndEmailAndName(String id, String email, String name);

    @Query("SELECT c FROM Customer c " +
        "LEFT JOIN FETCH c.rankEntity")
    Page<Customer> findCustomerAndRank(Pageable pageable);

    List<Customer> findAll();
    
    // 닉네임으로 고객 검색
    @Query("SELECT c FROM Customer c " +
           "WHERE c.nickname LIKE %:nickname% " +
           "AND c.status = 0 " +
           "ORDER BY c.customerIdx ASC")
    List<Customer> findByNicknameContaining(@Param("nickname") String nickname);

    // 해당 호텔을 이용한 고객 중에서 닉네임으로 검색
    @Query("SELECT DISTINCT c FROM Customer c " +
           "INNER JOIN RoomReservation rr ON c.customerIdx = rr.customerIdx " +
           "WHERE rr.contentid = :contentId " +
           "AND c.status = 0 " +
           "AND (c.nickname LIKE %:nickname% OR c.name LIKE %:nickname% OR c.email LIKE %:nickname%) " +
           "ORDER BY c.customerIdx ASC")
    List<Customer> findByContentIdAndSearchTerm(@Param("contentId") String contentId, @Param("nickname") String nickname);

    Optional<Customer> findByRefTokenAndId(String tokenID, String id);

    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByEmailAndStatus(String email, Integer status);

    Optional<Customer> findByCustomerIdxAndStatus(Integer customerIdx, Integer status);

    /**
     * 이번달 신규 가입 회원 수 조회
     */
    @Query("SELECT COUNT(c) FROM Customer c " +
           "WHERE c.status = 0 " +
           "AND YEAR(c.joinDate) = YEAR(CURRENT_DATE) " +
           "AND MONTH(c.joinDate) = MONTH(CURRENT_DATE)")
    Long countNewCustomersThisMonth();

    /**
     * 고객 등급 업데이트 스케줄러용: customerIdx와 totalPrice만 조회
     */
    @Query("SELECT c.customerIdx, c.totalPrice FROM Customer c WHERE c.status = 0")
    List<Object[]> findAllCustomerIdxAndTotalPrice();

    /**
     * 특정 고객의 현재 등급 조회
     */
    @Query("SELECT c.rank FROM Customer c WHERE c.customerIdx = :customerIdx")
    String findRankByCustomerIdx(@Param("customerIdx") Integer customerIdx);

    /**
     * 특정 고객의 등급 업데이트
     */
    @Modifying
    @Query("UPDATE Customer c SET c.rank = :rank WHERE c.customerIdx = :customerIdx")
    void updateRankByCustomerIdx(@Param("customerIdx") Integer customerIdx, @Param("rank") String rank);

    /**
     * 등급별 활성 고객 조회 (status = 0)
     */
    @Query("SELECT c FROM Customer c WHERE c.rank = :rank AND c.status = 0")
    List<Customer> findByRankAndStatus(@Param("rank") String rank);

    /**
     * 등급별 활성 고객 수 조회 (status = 0)
     */
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.rank = :rank AND c.status = 0")
    Long countByRankAndStatus(@Param("rank") String rank);
}
