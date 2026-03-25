package com.example.interview.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;

    @TempDir
    Path tempDir;

    private Path inputFile;
    private Path outputDir;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        
        inputFile = tempDir.resolve("test-data.csv");
        outputDir = tempDir.resolve("output");
        
        // Set private fields using reflection
        ReflectionTestUtils.setField(reportService, "inputPath", inputFile.toString());
        ReflectionTestUtils.setField(reportService, "outputDir", outputDir.toString());
        
        Files.createDirectories(outputDir);
    }

    @Test
    void testGenerateAllReports_WithValidData() throws IOException {
        // Given: Valid CSV data with different CTR and CPA values
        String csvContent = """
            campaign_id,date,impressions,clicks,spend,conversions
            CMP001,2025-01-01,10000,300,150.00,12
            CMP002,2025-01-01,20000,500,250.00,10
            CMP003,2025-01-01,15000,600,180.00,18
            CMP004,2025-01-01,8000,200,120.00,8
            CMP005,2025-01-01,12000,480,200.00,16
            """;
        Files.writeString(inputFile, csvContent);

        // When: Generate reports
        reportService.generateAllReports();

        // Then: Verify both output files are created
        Path ctrFile = outputDir.resolve("top10_ctr.csv");
        Path cpaFile = outputDir.resolve("top10_cpa.csv");
        
        assertTrue(Files.exists(ctrFile));
        assertTrue(Files.exists(cpaFile));
        assertTrue(Files.size(ctrFile) > 100); // Should have meaningful content
        assertTrue(Files.size(cpaFile) > 100); // Should have meaningful content
    }

    @Test
    void testGenerateAllReports_WithZeroConversions() throws IOException {
        // Given: Data with some campaigns having zero conversions
        String csvContent = """
            campaign_id,date,impressions,clicks,spend,conversions
            CMP001,2025-01-01,10000,300,150.00,12
            CMP002,2025-01-01,20000,500,250.00,0
            CMP003,2025-01-01,15000,600,180.00,0
            """;
        Files.writeString(inputFile, csvContent);

        // When: Generate reports
        reportService.generateAllReports();

        // Then: CPA report should only include campaigns with conversions > 0
        Path cpaFile = outputDir.resolve("top10_cpa.csv");
        String cpaContent = Files.readString(cpaFile);
        
        assertTrue(cpaContent.contains("CMP001")); // Should include CMP001 (has conversions)
        assertFalse(cpaContent.contains("CMP002")); // Should exclude CMP002 (0 conversions)
        assertFalse(cpaContent.contains("CMP003")); // Should exclude CMP003 (0 conversions)
    }

    @Test
    void testGenerateAllReports_EmptyFile() throws IOException {
        // Given: CSV file with only header (DuckDB can't infer types from empty data)
        String csvContent = """
            campaign_id,date,impressions,clicks,spend,conversions
            """;
        Files.writeString(inputFile, csvContent);

        // When & Then: Should handle empty data gracefully
        // Note: DuckDB throws exception for empty data, which is expected behavior
        assertThrows(RuntimeException.class, () -> reportService.generateAllReports(),
            "Should throw exception for empty CSV data - this is expected DuckDB behavior");
    }

    @Test
    void testCtrCalculation() throws IOException {
        // Given: Data with known CTR values for verification
        String csvContent = """
            campaign_id,date,impressions,clicks,spend,conversions
            CMP_HIGH_CTR,2025-01-01,10000,500,150.00,10
            CMP_LOW_CTR,2025-01-01,10000,100,150.00,10
            """;
        Files.writeString(inputFile, csvContent);

        // When: Generate reports
        reportService.generateAllReports();

        // Then: Verify CTR calculations
        Path ctrFile = outputDir.resolve("top10_ctr.csv");
        String content = Files.readString(ctrFile);
        
        // CMP_HIGH_CTR: 500/10000 = 0.05
        // CMP_LOW_CTR: 100/10000 = 0.01
        assertTrue(content.contains("0.05"), "Should contain high CTR value (0.05)");
        assertTrue(content.contains("0.01"), "Should contain low CTR value (0.01)");
        
        // High CTR should appear first (descending order)
        int highCtrIndex = content.indexOf("CMP_HIGH_CTR");
        int lowCtrIndex = content.indexOf("CMP_LOW_CTR");
        assertTrue(highCtrIndex < lowCtrIndex, "High CTR campaign should appear before low CTR");
    }

    @Test
    void testCpaCalculation() throws IOException {
        // Given: Data with known CPA values for verification
        String csvContent = """
            campaign_id,date,impressions,clicks,spend,conversions
            CMP_HIGH_CPA,2025-01-01,10000,300,200.00,5
            CMP_LOW_CPA,2025-01-01,10000,300,100.00,10
            """;
        Files.writeString(inputFile, csvContent);

        // When: Generate reports
        reportService.generateAllReports();

        // Then: Verify CPA calculations
        Path cpaFile = outputDir.resolve("top10_cpa.csv");
        String content = Files.readString(cpaFile);
        
        // CMP_HIGH_CPA: 200.00/5 = 40.00
        // CMP_LOW_CPA: 100.00/10 = 10.00
        assertTrue(content.contains("40.0"), "Should contain high CPA value (40.0)");
        assertTrue(content.contains("10.0"), "Should contain low CPA value (10.0)");
        
        // Low CPA should appear first (ascending order)
        int lowCpaIndex = content.indexOf("CMP_LOW_CPA");
        int highCpaIndex = content.indexOf("CMP_HIGH_CPA");
        assertTrue(lowCpaIndex < highCpaIndex, "Low CPA campaign should appear before high CPA");
    }

    @Test
    void testOutputDirectoryCreation() throws IOException {
        // Given: Non-existent output directory
        Path nonExistentDir = tempDir.resolve("non-existent-output");
        ReflectionTestUtils.setField(reportService, "outputDir", nonExistentDir.toString());
        
        String csvContent = """
            campaign_id,date,impressions,clicks,spend,conversions
            CMP001,2025-01-01,10000,300,150.00,12
            """;
        Files.writeString(inputFile, csvContent);

        // When: Generate reports
        assertDoesNotThrow(() -> reportService.generateAllReports());

        // Then: Output directory should be created
        assertTrue(Files.exists(nonExistentDir), "Output directory should be created");
        assertTrue(Files.isDirectory(nonExistentDir), "Should be a directory");
    }
}
