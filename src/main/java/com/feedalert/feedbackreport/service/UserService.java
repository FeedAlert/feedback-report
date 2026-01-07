package com.feedalert.feedbackreport.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final DataSource dataSource;

    public List<String> getAdminEmails() throws SQLException {
        log.info("Collecting administrator emails from the database...");
        List<String> emails = new ArrayList<>();

        String sql = "SELECT email FROM tb_user as u " +
                "JOIN tb_role as r ON u.role_id = r.role_id " +
                "WHERE r.name = 'ADMIN'";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) emails.add(rs.getString("email"));
        }
        return emails;
    }

}
