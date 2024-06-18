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
                "ffmpeg", "-i", inputFilePath,
                "-map", "0:v", "-map", "0:a",
                "-map", "0:v", "-b:v:0", "3000k", "-s:v:0", "1920x1080", // Full HD
                "-map", "0:v", "-b:v:1", "1500k", "-s:v:1", "1280x720",  // HD
                "-map", "0:v", "-b:v:2", "800k",  "-s:v:2", "854x480",   // SD
                "-c:v", "libx264", "-c:a", "aac",
                "-crf", "10",
                "-f", "dash",
                "-seg_duration", "4",
                "-use_template", "1",
                "-use_timeline", "1",
                "-init_seg_name", initSegmentPath,
                "-media_seg_name", mediaSegmentPath,
                outputFilePath
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
