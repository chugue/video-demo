package com.project.videodemo.video;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

@Service
public class VideoService {

    @Transactional
    public void encode(String inputFilePath, String baseFileName, Path directoryPath) throws IOException, InterruptedException {
        // 파일 이름을 소문자로 변환하고, 공백을 하이픈으로 대체

        String outputFilePath = directoryPath.resolve(baseFileName + ".mpd").toString();
        String initSegmentPath = "videolocation/" + baseFileName + "/" + baseFileName + "_init_$RepresentationID$.m4s";
        String mediaSegmentPath = "videolocation/" + baseFileName + "/" + baseFileName + "_chunk_$RepresentationID$_$Number%05d$.m4s";

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-i", inputFilePath,                       // 입력 파일 경로
                "-map", "0:v", "-map", "0:a",              // 모든 비디오와 오디오 스트림을 맵핑
                "-b:v:0", "3000k", "-s:v:0", "1920x1080", // 첫 번째 비디오 스트림: 3000kbps, 1920x1080 (Full HD)
                "-b:v:1", "1500k", "-s:v:1", "1280x720",  // 두 번째 비디오 스트림: 1500kbps, 1280x720 (HD)
                "-b:v:2", "800k",  "-s:v:2", "854x480",   // 세 번째 비디오 스트림: 800kbps, 854x480 (SD)
                "-c:v", "libx264",                        // 비디오 코덱: libx264 (H.264)
                "-c:a", "aac",                            // 오디오 코덱: AAC
                "-f", "dash",                             // 출력 포맷: DASH
                "-seg_duration", "4",                     // 세그먼트 길이: 4초
                "-use_template", "1",                     // 템플릿 사용
                "-use_timeline", "1",                     // 타임라인 사용
                "-init_seg_name", initSegmentPath,        // 초기화 세그먼트 파일 이름
                "-preset", "faster",
                "-media_seg_name", mediaSegmentPath,      // 미디어 세그먼트 파일 이름
                "-threads", "0",
                outputFilePath                            // 출력 파일 경로
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        // 별도의 스레드에서 FFmpeg 출력 읽기
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // 프로세스의 종료를 기다림
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg command failed with exit code " + exitCode);
        }
    }
}
