package com.feedalert.feedbackreport;

import com.feedalert.feedbackreport.dto.ReportData;
import com.feedalert.feedbackreport.service.EmailService;
import com.feedalert.feedbackreport.service.ReportService;
import com.feedalert.feedbackreport.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WeeklyReportFunction {

    private final ReportService reportService;
    private final UserService userService;
    private final EmailService emailService;

    @Bean("weeklyReport")
    public Function<Void, String> weeklyReport() {
        return unused -> {
            try {
                log.info("Starting weekly report...");

                var admins = userService.getAdminEmails();
                if (admins.isEmpty()) {
                    log.warn("No administrators found to send the report.");
                    return "No administrators found.";
                }

                log.info("{} Administrators found - sending report...", admins.size());
                ReportData data = reportService.generateWeeklyData();
                byte[] pdf = reportService.createPdf(data);
                emailService.sendEmail(admins, pdf);

                log.info("Weekly report was generated and sent successfully.");

                return "Weekly report generated and sent successfully.";

            } catch (Exception e) {
                log.error("Error generating report - {}", e.getMessage(), e);
                return "Error generating weekly report: " + e.getMessage();
            }
        };
    }

}
