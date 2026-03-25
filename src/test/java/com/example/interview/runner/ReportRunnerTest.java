package com.example.interview.runner;

import com.example.interview.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportRunnerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportRunner reportRunner;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRunCallsReportService() {
        // Given: ReportRunner with mocked ReportService
        
        // When: Call run method
        assertDoesNotThrow(() -> reportRunner.run());

        // Then: Should call generateAllReports once
        verify(reportService, times(1)).generateAllReports();
    }

    @Test
    void testRunWithArguments() {
        // Given: Arguments passed to run method
        String[] args = {"arg1", "arg2"};
        
        // When: Call run method with arguments
        assertDoesNotThrow(() -> reportRunner.run(args));

        // Then: Should still call generateAllReports
        verify(reportService, times(1)).generateAllReports();
    }

    @Test
    void testRunHandlesServiceException() {
        // Given: ReportService throws exception
        doThrow(new RuntimeException("Test exception")).when(reportService).generateAllReports();
        
        // When & Then: Should propagate exception
        assertThrows(RuntimeException.class, () -> reportRunner.run());
        verify(reportService, times(1)).generateAllReports();
    }
}
