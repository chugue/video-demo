package com.project.videodemo.video;


import ch.qos.logback.classic.Logger;
import com.project.videodemo._core.ApiUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@RequiredArgsConstructor
@Controller
public class VideoController {
    private final VideoService videoService;
    private static final String UPLOAD_DIR = "videolocation/";
    private final Path videoLocation = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();


    @GetMapping("/")
    public String index() {
        return "index";
    }


    @GetMapping("/videos")
    public ResponseEntity<?> getVideo(@RequestParam("filename") String filename) {
        try {
            // URL 인코딩된 디렉토리와 파일 이름을 디코딩
            String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8.toString());
            // 확장자 위치를 찾음
            int extensionIndex = decodedFilename.lastIndexOf('.');

            // 전체 파일 경로 조합
            String decodedDirectoryName = decodedFilename.substring(0, extensionIndex);
            Path filePath = videoLocation.resolve(Paths.get(decodedDirectoryName, decodedFilename).normalize());
            System.out.println("Requested file path: " + filePath.toString());

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                System.out.println("filename : " + resource.getFilename());
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .contentType(MediaType.parseMediaType("application/dash+xml"))
                        .body(resource);
            } else {
                System.out.println("File not found or not readable: " + filePath.toString());
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @CrossOrigin(origins = "http://localhost:8080")
    @PostMapping("/upload")
    public ResponseEntity<?> singleFileUpload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new ResponseEntity<>("Please select a file!", HttpStatus.BAD_REQUEST);
        }

        try {
            // 파일이름을 확장자로부터 분리, 없다면 output 이름으로 지정
            String originalFileName = file.getOriginalFilename();
            String baseFileName = originalFileName != null ? originalFileName.substring(0, originalFileName.lastIndexOf('.')) : "output";
            String sanitizedBaseFileName = baseFileName.toLowerCase().replaceAll("\\s+", "_");

            // 파일 명에 따라 디렉토리 생성
            Path videoLocation = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            Path directoryPath = videoLocation.resolve(sanitizedBaseFileName);

            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            // 파일을 서버에 저장
            Path targetLocation = directoryPath.resolve(sanitizedBaseFileName + ".mp4");
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // FFmpeg 명령어 준비 - 진짜 피일이름과, 어떤 이름으로 변환할 것인지에 대해서 명시해야된다.

            videoService.encode(targetLocation.toString(), sanitizedBaseFileName, directoryPath);


            // mpd파일에서 m4s호출 경로 수정 CORS 걸림
            Path mpdFilePath = directoryPath.resolve(sanitizedBaseFileName + ".mpd");
            RespDTO respDTO = new RespDTO(mpdFilePath);


            return ResponseEntity.ok(new ApiUtil<>(respDTO));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File processing failed: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    @Data
    public class RespDTO {
        private String filePath;

        public RespDTO(Path mpdFilePath) {
            this.filePath = mpdFilePath.toString();
        }
    }
}

