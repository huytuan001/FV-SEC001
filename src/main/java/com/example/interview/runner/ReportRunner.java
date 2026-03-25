package com.example.interview.runner;

import com.example.interview.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ReportRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ReportRunner.class);

    private final ReportService reportService;

    public ReportRunner(ReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    public void run(String... args) {
        log.info("=== Campaign Analytics Report Generator ===");
        log.info("Starting report generation...");
        reportService.generateAllReports();
        log.info("=== Done ===");
    }
}

