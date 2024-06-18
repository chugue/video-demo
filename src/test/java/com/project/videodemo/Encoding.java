package com.project.videodemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.nio.file.Path;
import java.nio.file.Paths;

@DataJpaTest
public class Encoding {

    @Test
    public void encode_test() {
        // given
        String originalFileName = "hello.mp4";
        String baseFileName = originalFileName != null ? originalFileName.substring(0, originalFileName.lastIndexOf('.')) : "output";
        String sanitizedBaseFileName = baseFileName.toLowerCase().replaceAll("\\s+", "-");
        String UPLOAD_DIR = "videolocation/";
        Path videoLocation = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
        Path directoryPath = videoLocation.resolve(sanitizedBaseFileName);
        // when
        String initSegmentPath = "videolocation/" + baseFileName + "/" + baseFileName + "_init_$RepresentationID$.m4s";
        String mediaSegmentPath = baseFileName + "_chunk_$RepresentationID$_$Number%05d$.m4s";
        // eye

        System.out.println("initSegmentPath : " + initSegmentPath);
        System.out.println("mediaSegmentPath : " + mediaSegmentPath);
        // then

    }
}

