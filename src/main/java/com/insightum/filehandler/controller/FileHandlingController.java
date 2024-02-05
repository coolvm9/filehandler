package com.insightum.filehandler.controller;

import com.insightum.filehandler.service.FileProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

@RestController
public class FileHandlingController {
    private static final Logger logger = LoggerFactory.getLogger(FileHandlingController.class);

    private static String  serverHost = "localhost";
    private static int  serverPort = 5052;

    @Autowired
    private FileProcessingService fileProcessingService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10); // Adjust thread pool size as needed

    @PostMapping("/uploadSynch")
    public ResponseEntity<Resource> handleFileUploadSynch(
            @RequestParam("file") MultipartFile file,
            @RequestParam Map<String, String> metadata) {
        if (file.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        metadata.forEach((key, value) -> logger.info("{}: {}", key, value));
        try {
            final PipedOutputStream pos = new PipedOutputStream();
            final PipedInputStream pis = new PipedInputStream(pos);

            // Submit a task to process the file and write to the PipedOutputStream
            Future<?> processingTask = executorService.submit(() -> {
                try {
                    fileProcessingService.processFileSynch(metadata,file.getInputStream(), pos);
                } catch (IOException e) {
                    logger.error("Failed to process file: {}", e.getMessage(), e);
                } finally {
                    try {
                        pos.close(); // Ensure pos is closed here after all writing is done
                    } catch (IOException e) {
                        logger.error("Error closing PipedOutputStream: {}", e.getMessage(), e);
                    }
                }
            });
            // Wait for the processing task to complete
            long timeout = 10L; // Timeout period
            TimeUnit timeUnit = TimeUnit.SECONDS; // Timeout unit
            processingTask.get(timeout, timeUnit);
            // Create a ResponseEntity with the InputStreamResource from the PipedInputStream
            InputStreamResource fileResource = new InputStreamResource(pis);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                    .body(fileResource);
        } catch (Exception e) {
            logger.error("Error processing file: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/uploadAsynch")
    public DeferredResult<ResponseEntity<?>> handleFileUploadAsynch(
            @RequestParam("file") MultipartFile file,
            @RequestParam Map<String, String> metadata) {
        long timeout = 10000L; // 10 seconds
        DeferredResult<ResponseEntity<?>> deferredResult = new DeferredResult<>(timeout);
        try {
            final PipedOutputStream pos = new PipedOutputStream();
            final PipedInputStream pis = new PipedInputStream(pos, 4096);
            // Optionally increase buffer size
            CompletableFuture.runAsync(() -> {
                try {
                    fileProcessingService.processFileAsync(metadata, file.getInputStream(), pos)
                            .thenAccept(justVoid -> {
                               try {
                                    pos.close(); // Close here after writing is done

                                } catch (IOException e) {
                                    logger.error("Error closing PipedOutputStream", e);
                                }
                            });
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.error("Error processing file asynchronously", e);
                }
            });
            // Read from the PipedInputStream in the current thread or another async task
            CompletableFuture<Void> readFuture = CompletableFuture.runAsync(() -> {
                // Convert PipedInputStream to InputStreamResource for the response
                InputStreamResource inputStreamResource = new InputStreamResource(pis);
                ResponseEntity<?> responseEntity = ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                        .body(inputStreamResource);
                deferredResult.setResult(responseEntity);
//                    pis.close(); // Ensure the InputStream is closed after consumption
            });

            readFuture.thenRun(() -> {
                try {
                    pis.close();
                } catch (IOException e) {
                    logger.error("Error closing PipedInputStream", e);
                }
            });
        } catch (IOException e) {
            deferredResult.setErrorResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error setting up file processing streams"));
        }
        return deferredResult;
    }

    @PostMapping("/uploadAsynch1")
    public DeferredResult<ResponseEntity<?>> handleFileUploadAsynch1(
            @RequestParam("file") MultipartFile file,
            @RequestParam Map<String, String> metadata) {
        long timeout = 10000L; // 10 seconds
        DeferredResult<ResponseEntity<?>> deferredResult = new DeferredResult<>(timeout);

        try {
            final PipedOutputStream pos = new PipedOutputStream();
            final PipedInputStream pis = new PipedInputStream(pos, 4096); // Optionally increase buffer size

            CompletableFuture<Void> processingTask = CompletableFuture.runAsync(() -> {
                try {
                    fileProcessingService.processFileAsync(metadata, file.getInputStream(), pos)
                            .thenAccept(justVoid -> {
                                // Processing completed
                            });
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.error("Error processing file asynchronously", e);
                } finally {
                    try {
                        pos.close(); // Close the PipedOutputStream after writing is done
                    } catch (IOException e) {
                        logger.error("Error closing PipedOutputStream", e);
                    }
                }
            });

            CompletableFuture<Void> readFuture = CompletableFuture.runAsync(() -> {
                // Corrected class name here
                InputStreamResource inputStreamResource = new InputStreamResource(pis);
                ResponseEntity<?> responseEntity = ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                        .body(inputStreamResource);
                deferredResult.setResult(responseEntity);
            });

            // Combine the processing and reading tasks
            CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(processingTask, readFuture);

            // Handle exceptions
            combinedFuture.exceptionally(ex -> {
                deferredResult.setErrorResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing file: " + ex.getMessage()));
                return null;
            });
        } catch (IOException e) {
            deferredResult.setErrorResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error setting up file processing streams"));
        }

        return deferredResult;
    }


}
