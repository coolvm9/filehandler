package com.insightum.filehandler;

import com.insightum.filehandler.controller.FileHandlingController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FilehandlerApplication {
	private static final Logger logger = LoggerFactory.getLogger(FilehandlerApplication.class);

	public static void main(String[] args) {
		logger.debug("Debug...");
		logger.info("Info...");

		SpringApplication.run(FilehandlerApplication.class, args);
	}

}
