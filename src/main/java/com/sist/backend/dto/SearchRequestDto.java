package com.sist.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequestDto {
    private String mainCategory;
    private String subCategory;
    private Integer status;
    private Integer priority;
    private Integer customerIdx;
    private Integer adminIdx;
    private String contentId;
    private String title;
    private int page = 0;
    private int size = 10;
}
