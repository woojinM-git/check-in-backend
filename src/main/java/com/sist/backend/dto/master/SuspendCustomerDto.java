package com.sist.backend.dto.master;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuspendCustomerDto {
    private Integer customerIdx;
    private String reason;
}

