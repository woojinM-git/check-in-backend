package com.sist.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
