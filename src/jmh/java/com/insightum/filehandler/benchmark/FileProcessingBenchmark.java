package com.insightum.filehandler.benchmark;

import com.insightum.filehandler.client.FileClient;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Thread)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class FileProcessingBenchmark {


    private FileClient client;
    @Param({"smallFile", "largeFile"})
    private String fileType;
    private ByteArrayInputStream inputStream;

    @Setup(Level.Trial)
    public void setup() {
        // Initialize your FileProcessingService here if it has dependencies
        client = new FileClient();
        // Setup test data based on the fileType
        String testData = "smallFile".equals(fileType) ? "Short string" : new String(new char[1000000]).replace('\0', 'A');
        inputStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    }


    @Benchmark
    public void benchmarkProcessFile() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            client.processFileSynch(inputStream, outputStream);
            // Optionally verify the output here
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
