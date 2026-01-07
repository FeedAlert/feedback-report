package com.feedalert.feedbackreport.service;

import com.feedalert.feedbackreport.dto.ReportData;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final DataSource dataSource;

    public ReportData generateWeeklyData() throws SQLException {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);
        ReportData data = new ReportData();
        data.setReportStartDate(startDate);
        data.setReportEndDate(endDate);

        String sql = "SELECT rating, is_urgent, created_at, \"comment\" FROM tb_feedback WHERE created_at >= ? AND created_at < ?";
        double ratingSum = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(startDate.atStartOfDay()));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate.plusDays(1).atStartOfDay()));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    data.setTotalFeedbacks(data.getTotalFeedbacks() + 1);
                    ratingSum += rs.getInt("rating");
                    if (rs.getBoolean("is_urgent")) data.setTotalUrgent(data.getTotalUrgent() + 1);
                    String comment = rs.getString("comment");
                    if (comment != null && !comment.isBlank()) data.getComments().add(comment);
                    LocalDate day = rs.getTimestamp("created_at").toLocalDateTime().toLocalDate();
                    data.getFeedbacksPerDay().merge(day, 1L, Long::sum);
                }
            }
        }
        if (data.getTotalFeedbacks() > 0) data.setAverageRating(ratingSum / data.getTotalFeedbacks());

        log.info("Weekly report data generated: {} feedbacks from {} to {}", data.getTotalFeedbacks(), startDate, endDate);
        return data;
    }

    public byte[] createPdf(ReportData data) throws IOException {
        String html;
        log.info("Generating PDF report...");
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("report-template.html")) {
            assert in != null;
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        String averageRatingText;
        String ratingClass;

        if (data.getTotalFeedbacks() == 0) {
            averageRatingText = "-";
            ratingClass = "";
        } else {
            double avg = data.getAverageRating();
            averageRatingText = String.format("%.1f", avg);

            if (avg >= 4.5) {
                ratingClass = "rating-excellent";
            } else if (avg >= 3.5) {
                ratingClass = "rating-good";
            } else if (avg >= 2.5) {
                ratingClass = "rating-regular";
            } else {
                ratingClass = "rating-bad";
            }
        }

        html = html.replace("{{RATING_CLASS}}", ratingClass);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        html = html.replace("{{REPORT_DATE_RANGE}}", "Período: " + data.getReportStartDate().format(fmt) + " a " + data.getReportEndDate().format(fmt))
                .replace("{{AVERAGE_RATING}}", averageRatingText)
                .replace("{{TOTAL_FEEDBACKS}}", String.valueOf(data.getTotalFeedbacks()))
                .replace("{{TOTAL_URGENT}}", String.valueOf(data.getTotalUrgent()))
                .replace("{{TOTAL_NORMAL}}", String.valueOf(data.getTotalNonUrgentFeedbacks()));

        String dayList = data.getFeedbacksPerDay().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> "<li>" + e.getKey() + ": <b>" + e.getValue() + "</b> avaliações</li>")
                .collect(Collectors.joining());
        html = html.replace("{{FEEDBACKS_PER_DAY_LIST}}", dayList.isEmpty() ? "<li>Sem dados</li>" : dayList);

        String commentList = data.getComments().stream()
                .map(c -> "<li>" + c.replace("<", "&lt;").replace(">", "&gt;") + "</li>")
                .collect(Collectors.joining());
        html = html.replace("{{COMMENTS_LIST}}", commentList.isEmpty() ? "<li>Sem comentários</li>" : commentList);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        }
    }

}
