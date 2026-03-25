package com.example.interview.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    @Value("${app.csv.input-path}")
    private String inputPath;

    @Value("${app.csv.output-dir}")
    private String outputDir;

    public void generateAllReports() {
        try {
            Path outDir = Paths.get(outputDir);
            if (!Files.exists(outDir)) {
                Files.createDirectories(outDir);
                log.info("Created output directory: {}", outDir.toAbsolutePath());
            }

            String absoluteInput = Paths.get(inputPath).toAbsolutePath().toString();
            String absoluteOutput = outDir.toAbsolutePath().toString();

            try (Connection conn = DriverManager.getConnection("jdbc:duckdb:")) {
                long start = System.currentTimeMillis();

                generateTopCtrReport(conn, absoluteInput, absoluteOutput);
                generateLowestCpaReport(conn, absoluteInput, absoluteOutput);

                long elapsed = System.currentTimeMillis() - start;
                log.info("All reports generated in {} ms", elapsed);
            }
        } catch (SQLException | IOException e) {
            log.error("Failed to generate reports", e);
            throw new RuntimeException(e);
        }
    }

    private void generateTopCtrReport(Connection conn, String inputFile, String outputDir)
        throws SQLException {
        String outputFile = outputDir + "/top10_ctr.csv";

        String sql = """
            COPY (
                SELECT
                    campaign_id,
                    SUM(impressions)                                    AS total_impressions,
                    SUM(clicks)                                         AS total_clicks,
                    ROUND(SUM(spend), 2)                                AS total_spend,
                    SUM(conversions)                                    AS total_conversions,
                    ROUND(SUM(clicks) * 1.0 / SUM(impressions), 4)      AS CTR,
                    ROUND(SUM(spend) / NULLIF(SUM(conversions), 0), 2)  AS CPA
                FROM read_csv_auto('%s')
                GROUP BY campaign_id
                HAVING SUM(impressions) > 0
                ORDER BY CTR DESC
                LIMIT 10
            ) TO '%s' (HEADER, DELIMITER ',');
            """.formatted(inputFile, outputFile);

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("Report generated: {}", outputFile);
        }
    }

    private void generateLowestCpaReport(Connection conn, String inputFile, String outputDir)
        throws SQLException {
        String outputFile = outputDir + "/top10_cpa.csv";

        String sql = """
            COPY (
                SELECT
                    campaign_id,
                    SUM(impressions)                                    AS total_impressions,
                    SUM(clicks)                                         AS total_clicks,
                    ROUND(SUM(spend), 2)                                AS total_spend,
                    SUM(conversions)                                    AS total_conversions,
                    ROUND(SUM(clicks) * 1.0 / SUM(impressions), 4)      AS CTR,
                    ROUND(SUM(spend) / NULLIF(SUM(conversions), 0), 2)  AS CPA
                FROM read_csv_auto('%s')
                GROUP BY campaign_id
                HAVING SUM(conversions) > 0
                ORDER BY CPA ASC
                LIMIT 10
            ) TO '%s' (HEADER, DELIMITER ',');
            """.formatted(inputFile, outputFile);

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("Report generated: {}", outputFile);
        }
    }

}

