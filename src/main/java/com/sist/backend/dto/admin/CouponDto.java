package com.sist.backend.dto.admin;

import java.time.LocalDateTime;

import com.sist.backend.entity.Coupon;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponDto {
    /* Coupon 기본 정보 */
    private Integer couponIdx;
    private Integer templateIdx;
    private Integer customerIdx;
    private Integer adminIdx;
    private LocalDateTime createDate;
    private LocalDateTime endDate;
    private Boolean status;

    /* CouponTemplate 정보 */
    private CouponTemplate couponTemplate;

    /* Customer 정보 */
    private Customer customer;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CouponTemplate {
        private String templateName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Customer {
        private String name;
    }

    public static CouponDto fromEntity(Coupon coupon) {
        CouponDto dto = new CouponDto();
        dto.setTemplateIdx(coupon.getTemplateIdx());
        dto.setCustomerIdx(coupon.getCustomerIdx());
        dto.setAdminIdx(coupon.getAdminIdx());
        dto.setCreateDate(coupon.getCreateDate());
        dto.setEndDate(coupon.getEndDate());
        dto.setStatus(coupon.getStatus());

        /* CouponTemplate 정보 */
        if (coupon.getCouponTemplate() != null) {
            CouponTemplate couponTemplate = new CouponTemplate();
            couponTemplate.setTemplateName(coupon.getCouponTemplate().getTemplateName());
            dto.setCouponTemplate(couponTemplate);
        }

        /* Customer 정보 */
        if (coupon.getCustomer() != null) {
            Customer customer = new Customer();
            customer.setName(coupon.getCustomer().getName());
            dto.setCustomer(customer);
        }

        return dto;
    }
}
