package com.feedalert.feedbackreport.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class ReportData {

    private double averageRating = 0.0;
    private int totalFeedbacks = 0;
    private int totalUrgent = 0;
    private Map<LocalDate, Long> feedbacksPerDay = new HashMap<>();
    private List<String> comments = new ArrayList<>();
    private LocalDate reportStartDate;
    private LocalDate reportEndDate;

    /**
     * Convenience method to calculate the total number of normal feedbacks.
     * @return The number of non-urgent feedbacks.
     */
    public int getTotalNonUrgentFeedbacks() {
        return totalFeedbacks - totalUrgent;
    }

}
