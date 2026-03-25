package com.example.interview;

import com.example.interview.config.TestConfig;
import com.example.interview.service.ReportService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(TestConfig.class)
class CampaignAnalyticsApplicationTests {

    @Autowired
    private ReportService reportService;

    @TempDir
    Path tempDir;

    private Path inputFile;
    private Path outputDir;

    @BeforeEach
    void setUp() throws IOException {
        inputFile = tempDir.resolve("test-data.csv");
        outputDir = tempDir.resolve("test-output");
        Files.createDirectories(outputDir);

        // Create sample test data
        String csvContent = """
            campaign_id,date,impressions,clicks,spend,conversions
            CMP001,2025-01-01,10000,275,100.50,10
            CMP002,2025-01-01,20000,400,200.75,5
            CMP003,2025-01-01,15000,450,150.25,15
            CMP004,2025-01-01,5000,100,75.00,0
            CMP005,2025-01-01,8000,320,120.80,8
            """;
        Files.writeString(inputFile, csvContent);

        // Configure ReportService using ReflectionTestUtils
        ReflectionTestUtils.setField(reportService, "inputPath", inputFile.toString());
        ReflectionTestUtils.setField(reportService, "outputDir", outputDir.toString());
    }

    @Test
    void contextLoads() {
        assertNotNull(reportService);
    }

    @Test
    void testReportServiceIsNotNull() {
        assertNotNull(reportService, "ReportService should be autowired");
    }

    @Test
    void testGenerateAllReportsCreatesOutputFiles() throws Exception {
        // Given: test data is set up in setUp()
        
        // When: generate reports
        reportService.generateAllReports();
        
        // Then: output files should exist
        Path ctrFile = outputDir.resolve("top10_ctr.csv");
        Path cpaFile = outputDir.resolve("top10_cpa.csv");
        
        assertTrue(Files.exists(ctrFile), "CTR report file should exist");
        assertTrue(Files.exists(cpaFile), "CPA report file should exist");
        
        // Verify files are not empty
        assertTrue(Files.size(ctrFile) > 0, "CTR report should not be empty");
        assertTrue(Files.size(cpaFile) > 0, "CPA report should not be empty");
    }

    @Test
    void testCtrReportContent() throws Exception {
        // Given: test data with known CTR values
        reportService.generateAllReports();
        
        // When: read CTR report
        Path ctrFile = outputDir.resolve("top10_ctr.csv");
        String content = Files.readString(ctrFile);
        
        // Then: verify content structure and data
        assertTrue(content.contains("campaign_id,total_impressions,total_clicks,total_spend,total_conversions,CTR,CPA"));
        assertTrue(content.contains("CMP003")); // Should have highest CTR (450/15000 = 0.03)
        
        // Verify CTR calculation for CMP003: 450/15000 = 0.03
        assertTrue(content.contains("0.03"), "Should contain correct CTR calculation");
    }

    @Test
    void testCpaReportContent() throws Exception {
        // Given: test data with known CPA values
        reportService.generateAllReports();
        
        // When: read CPA report
        Path cpaFile = outputDir.resolve("top10_cpa.csv");
        String content = Files.readString(cpaFile);
        
        // Then: verify content structure and excludes zero conversions
        assertTrue(content.contains("campaign_id,total_impressions,total_clicks,total_spend,total_conversions,CTR,CPA"));
        assertFalse(content.contains("CMP004")); // Should exclude CMP004 (0 conversions)
        
        // Verify CPA calculation for CMP001: 100.50/10 = 10.05
        assertTrue(content.contains("10.05"), "Should contain correct CPA calculation");
    }
}
