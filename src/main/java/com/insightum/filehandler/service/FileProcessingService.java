package com.insightum.filehandler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class FileProcessingService {

    private final Logger logger = LoggerFactory.getLogger(FileProcessingService.class);
    private static String  serverHost = "localhost";
    private static int  serverPort = 5052;
    @Async
    public CompletableFuture<Void> processFileAsync(Map<String, String> metadata,InputStream inputStream, OutputStream outputStream) {
        return CompletableFuture.runAsync(() -> {
            try (Socket socket = new Socket(serverHost, serverPort);
                 OutputStream socketOut = socket.getOutputStream();
                 InputStream socketIn = socket.getInputStream()) {
                for (Map.Entry<String, String> entry : metadata.entrySet()) {
                    socketOut.write((entry.getKey() + "=" + entry.getValue() + "\n").getBytes());
                }
                socketOut.write("END_METADATA\n".getBytes());

                // Send file content
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    socketOut.write(buffer, 0, bytesRead);
                }
                socketOut.flush();
                socket.shutdownOutput();

                // Read response and write to outputStream
                while ((bytesRead = socketIn.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                logger.error("Failed to send file to server: {}", e.getMessage());
                throw new RuntimeException("Failed to process file asynchronously", e);
            }
        });
    }

    public void processFileSynch(Map<String, String> metadata,InputStream inputStream, OutputStream outputStream) {
        try (Socket socket = new Socket(serverHost, serverPort);
             OutputStream socketOut = socket.getOutputStream();
             InputStream socketIn = socket.getInputStream()) {
//             Optionally, send metadata first if your server expects it
//            Map<String, String> metadata = new HashMap<>();
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                socketOut.write((entry.getKey() + "=" + entry.getValue() + "\n").getBytes());
            }
            socketOut.write("END_METADATA\n".getBytes());
            // Now, send the file content
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                socketOut.write(buffer, 0, bytesRead);
            }
            socketOut.flush();
            socket.shutdownOutput();
            // write the returned file from the server to outputstream
            bytesRead = 0; // Reset bytesRead to reuse the variable
            while ((bytesRead = socketIn.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            logger.error("Failed to send file to server: {}", e.getMessage());
            // Handle exception as necessary
        }
    }




}

