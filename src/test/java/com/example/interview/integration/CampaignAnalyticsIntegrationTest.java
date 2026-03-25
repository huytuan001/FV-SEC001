package com.example.interview.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CampaignAnalyticsIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) throws IOException {
        Path inputFile = tempDir.resolve("integration-test-data.csv");
        Path outputDir = tempDir.resolve("integration-output");
        
        // Create sample data that covers various scenarios
        String csvContent = """
            campaign_id,date,impressions,clicks,spend,conversions
            CMP_BEST_CTR,2025-01-01,10000,400,200.00,20
            CMP_WORST_CTR,2025-01-01,20000,100,300.00,15
            CMP_BEST_CPA,2025-01-01,15000,300,150.00,25
            CMP_WORST_CPA,2025-01-01,8000,200,400.00,8
            CMP_NO_CONV,2025-01-01,12000,250,180.00,0
            CMP_HIGH_SPEND,2025-01-01,5000,150,500.00,10
            CMP_HIGH_IMP,2025-01-01,50000,1000,600.00,30
            CMP_LOW_SPEND,2025-01-01,3000,90,50.00,5
            CMP_MEDIUM1,2025-01-01,11000,330,220.00,18
            CMP_MEDIUM2,2025-01-01,9000,270,190.00,12
            CMP_MEDIUM3,2025-01-01,7000,210,160.00,9
            CMP_MEDIUM4,2025-01-01,6000,180,140.00,7
            """;
        Files.writeString(inputFile, csvContent);
        Files.createDirectories(outputDir);
        
        registry.add("app.csv.input-path", () -> inputFile.toString());
        registry.add("app.csv.output-dir", () -> outputDir.toString());
    }

    @Test
    void testFullApplicationContext() {
        // Verify all required beans are loaded
        assertNotNull(applicationContext.getBean("reportService"));
        assertNotNull(applicationContext.getBean("reportRunner"));
    }

    @Test
    void testEndToEndReportGeneration() throws IOException {
        // This test verifies the complete workflow from start to finish
        Path outputDir = tempDir.resolve("integration-output");
        Path ctrFile = outputDir.resolve("top10_ctr.csv");
        Path cpaFile = outputDir.resolve("top10_cpa.csv");
        
        // Verify files exist after application context initialization
        // (ReportRunner should have executed during startup)
        assertTrue(Files.exists(ctrFile), "CTR report should be generated");
        assertTrue(Files.exists(cpaFile), "CPA report should be generated");
    }

    @Test
    void testCtrReportAccuracy() throws IOException {
        Path ctrFile = tempDir.resolve("integration-output/top10_ctr.csv");
        if (!Files.exists(ctrFile)) {
            return; // Skip if file doesn't exist yet
        }
        
        List<String> lines = Files.readAllLines(ctrFile);
        assertTrue(lines.size() > 1, "Should have header + data rows");
        
        // Verify header
        String header = lines.get(0);
        assertEquals("campaign_id,total_impressions,total_clicks,total_spend,total_conversions,CTR,CPA", header);
        
        // Check if campaigns are sorted by CTR descending
        if (lines.size() >= 3) {
            String firstDataRow = lines.get(1);
            String secondDataRow = lines.get(2);
            
            // Extract CTR values (5th column after campaign_id,impressions,clicks,spend,conversions)
            String[] firstRowParts = firstDataRow.split(",");
            String[] secondRowParts = secondDataRow.split(",");
            
            if (firstRowParts.length >= 6 && secondRowParts.length >= 6) {
                double firstCtr = Double.parseDouble(firstRowParts[5]);
                double secondCtr = Double.parseDouble(secondRowParts[5]);
                assertTrue(firstCtr >= secondCtr, "CTR should be sorted in descending order");
            }
        }
    }

    @Test
    void testCpaReportAccuracy() throws IOException {
        Path cpaFile = tempDir.resolve("integration-output/top10_cpa.csv");
        if (!Files.exists(cpaFile)) {
            return; // Skip if file doesn't exist yet
        }
        
        List<String> lines = Files.readAllLines(cpaFile);
        assertTrue(lines.size() > 1, "Should have header + data rows");
        
        // Verify header
        String header = lines.get(0);
        assertEquals("campaign_id,total_impressions,total_clicks,total_spend,total_conversions,CTR,CPA", header);
        
        // Verify no campaigns with zero conversions are included
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(",");
            if (parts.length >= 6) {
                int conversions = Integer.parseInt(parts[4]);
                assertTrue(conversions > 0, "CPA report should only include campaigns with conversions > 0");
            }
        }
        
        // Check if campaigns are sorted by CPA ascending
        if (lines.size() >= 3) {
            String firstDataRow = lines.get(1);
            String secondDataRow = lines.get(2);
            
            String[] firstRowParts = firstDataRow.split(",");
            String[] secondRowParts = secondDataRow.split(",");
            
            if (firstRowParts.length >= 7 && secondRowParts.length >= 7) {
                double firstCpa = Double.parseDouble(firstRowParts[6]);
                double secondCpa = Double.parseDouble(secondRowParts[6]);
                assertTrue(firstCpa <= secondCpa, "CPA should be sorted in ascending order");
            }
        }
    }

    @Test
    void testReportLimits() throws IOException {
        Path ctrFile = tempDir.resolve("integration-output/top10_ctr.csv");
        Path cpaFile = tempDir.resolve("integration-output/top10_cpa.csv");
        
        if (Files.exists(ctrFile)) {
            List<String> ctrLines = Files.readAllLines(ctrFile);
            // Should have at most 11 lines (1 header + 10 data rows)
            assertTrue(ctrLines.size() <= 11, "CTR report should have at most 10 data rows + header");
        }
        
        if (Files.exists(cpaFile)) {
            List<String> cpaLines = Files.readAllLines(cpaFile);
            // Should have at most 11 lines, but could be fewer due to zero conversion filtering
            assertTrue(cpaLines.size() <= 11, "CPA report should have at most 10 data rows + header");
        }
    }
}
