package com.insightum.filehandler.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class FileClient {
    private static final Logger logger = LoggerFactory.getLogger(FileClient.class);
    private static String  serverHost = "localhost";
    private static int  serverPort = 5052;
    private String filePath;

    public FileClient() {}

    public void processFileDUMMY(InputStream inputStream, ByteArrayOutputStream outputStream) {

    }
    public void processFileSynch(InputStream inputStream, OutputStream outputStream) {
        try (Socket socket = new Socket(serverHost, serverPort);
             OutputStream socketOut = socket.getOutputStream();
             InputStream socketIn = socket.getInputStream()) {
//             Optionally, send metadata first if your server expects it
            Map<String, String> metadata = new HashMap<>();
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
            logger.info("Completed File Processing");
        } catch (IOException e) {
            logger.error("Failed to send file to server: {}", e.getMessage());
            // Handle exception as necessary
        }
    }
}