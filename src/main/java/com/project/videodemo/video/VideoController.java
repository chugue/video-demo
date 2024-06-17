package com.project.videodemo.video;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Controller
public class VideoController {
    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);
    private final Path videoLocation = Paths.get("videolocation");

    public VideoController(){
        try {
            Files.createDirectories(videoLocation);
            Files.createDirectories(videoLocation.resolve("system"));
        } catch (IOException e) {
            throw new RuntimeException("could not create storage directory", e);
        }
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/upload")
    public String uploadVideo(@RequestParam("file") MultipartFile file){
        try {
            // 파일 저장
            Path targetLocation = videoLocation.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 원본 파일 이름에서 확장자를 제거한 이름 추출
            String originalFileName = file.getOriginalFilename();
            String baseFileName = originalFileName != null ? originalFileName.substring(0, originalFileName.lastIndexOf('.')) : "output";

            // FFmpeg를 사용하여 DASH로 변환
            String inputFilePath = targetLocation.toString();
            String outputDirPath = videoLocation.resolve("system").toString();
            String outputFilePath = String.format("%s/%s.mpd", outputDirPath, baseFileName);

            // FFmpeg 명령어 수정 (H.264 비디오 코덱과 AAC 오디오 코덱 사용)
            String command = String.format("ffmpeg -i %s -c:v libx264 -c:a aac -f dash %s", inputFilePath, outputFilePath);

            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            return "redirect:/";
        } catch (IOException e) {
            throw new RuntimeException("Fail to storage file", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    @GetMapping("/videos/{filename}")
    @ResponseBody
    public ResponseEntity<Resource> getVideo(@PathVariable String filename) {
        try {
            Path filePath = videoLocation.resolve("system").resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            String contentType = "application/dash+xml";
            if (filename.endsWith(".m4s")) {
                contentType = "video/iso.segment";
            } else if (filename.endsWith(".mp4")) {
                contentType = "video/mp4";
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(resource);
        } catch (IOException e) {
            logger.error("Failed to load file", e);
            return ResponseEntity.status(500).body(null);
        }
    }
}
