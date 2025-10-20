package com.sist.backend.dto.master;

import java.time.LocalDate;

import com.sist.backend.entity.Customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {
    // Customer 기본 정보
    private Integer customerIdx;
    private String id;
    private String rank;
    private LocalDate birthday;
    private String nickname;
    private String name;
    private String gender;
    private String password;
    private String phone;

    /* 회원 등급 정보 객체 */
    private Rank rankEntity;

    // 중첩 클래스들
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rank {
        private String rank;
        private Integer pointRate;
        private Integer yearCoupon;
        private Integer maxDiscount;
        private Integer conditions;
    }

    public static CustomerDto fromEntity(Customer customer) {
        CustomerDto dto = new CustomerDto();

        // Customer 기본 정보
        dto.setCustomerIdx(customer.getCustomerIdx());
        dto.setId(customer.getId());
        dto.setRank(customer.getRank());
        dto.setBirthday(customer.getBirthday());
        dto.setNickname(customer.getNickname());
        dto.setName(customer.getName());

        if(customer.getRankEntity() != null) {
            Rank rank = new Rank();
            rank.setRank(customer.getRankEntity().getRank());
            rank.setPointRate(customer.getRankEntity().getPointRate());
            rank.setYearCoupon(customer.getRankEntity().getYearCoupon());
            rank.setMaxDiscount(customer.getRankEntity().getMaxDiscount());
            rank.setConditions(customer.getRankEntity().getConditions());
            dto.setRankEntity(rank);
        }

        return dto;
    }
}
