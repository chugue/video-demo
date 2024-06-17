package com.project.videodemo.video;


import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Controller
public class VideoController {
    private final Path videoLocation = Paths.get("/videolocation");
    private static final Logger logger = (Logger) LoggerFactory.getLogger(VideoController.class);

    public VideoController() {
        try {
            Files.createDirectories(videoLocation);
            Files.createDirectories(videoLocation.resolve("system"));
            logger.info("Video storage directory created at: {}", videoLocation.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Could not create storage directory at: {}", videoLocation.toAbsolutePath(), e);
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/upload")
    public ResponseEntity<String> singleFileUpload (@RequestParam("file") MultipartFile file) {

        try {
            // 파일이름을 확장자로부터 분리, 없다면 output 이름으로 지정
            String originalFileName = file.getOriginalFilename();
            String baseFileName = originalFileName != null ? originalFileName.substring(0, originalFileName.lastIndexOf('.')) : "output";

            // 파일 명에 따라 디렉토리 생성
            Path directoryPath = videoLocation.resolve(baseFileName);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            // 파일을 서버에 저장
            Path targetLocation =directoryPath.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // FFmpeg 명령어 준비 - 진짜 피일이름과, 어떤 이름으로 변환할 것인지에 대해서 명시해야된다.
            String inputFilePath = targetLocation.toString();
            String outputFileName = baseFileName + ".mpd";
            String outputFilePath = directoryPath.resolve(outputFileName).toString(); // 수정된 부분: 출력 파일 경로에 파일 이름 지정

            // FFmpeg 명령어 배열 준비
            String[] command = { "ffmpeg", "-i", inputFilePath, "-c:v", "libx264", "-c:a", "aac", "-f", "dash", outputFilePath };

            // FFmpeg 명령어 실행
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg command failed with exit code " + exitCode);
            }

            return ResponseEntity.ok("File uploaded and processed successfully");
        } catch (IOException e) {
            logger.error("File processing failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File processing failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("FFmpeg command was interrupted", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("FFmpeg command was interrupted: " + e.getMessage());
        }

    }
}
