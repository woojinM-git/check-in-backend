package com.sist.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackStatsDto {
    private Long totalFeedback;
    private Long urgentFeedback;
    private Long inProgressFeedback;
    private Long resolvedFeedback;
}

