package com.videoslicer.video_slicer.application.usecases.impl;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Service;

import com.videoslicer.video_slicer.application.usecases.SliceVideoUseCase;

@Service
public class SliceVideoUseCaseImpl implements SliceVideoUseCase {

    private final String basePath = "src/main/videos/";

    @Override
    public SliceResult execute(String fileName, int timeIntervalInSeconds) {
        File videoFile = new File(basePath + fileName);
        if (!videoFile.exists()) {
            return new SliceResult(false, "Arquivo de vídeo não encontrado!", null);
        }

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile.getAbsolutePath())) {
            grabber.start();

            Frame frame;
            long timeIntervalInMicros = (long) timeIntervalInSeconds * 1_000_000L;
            long timestamp = 0L;
            int frameNumber = 0;
            Java2DFrameConverter converter = new Java2DFrameConverter();

            String outputPrefix = "sliced_" + fileName + "_frame_";

            while (timestamp < grabber.getLengthInTime()) {
                grabber.setTimestamp(timestamp);
                frame = grabber.grabImage();
                BufferedImage bi = converter.convert(frame);

                if (bi != null) {
                    File output = new File(basePath + outputPrefix + frameNumber + ".png");
                    ImageIO.write(bi, "png", output);
                }

                timestamp += timeIntervalInMicros;
                frameNumber++;
            }

            converter.close();
            grabber.stop();

            return new SliceResult(true, "Vídeo cortado com sucesso!", outputPrefix);
        } catch (Exception e) {
            e.printStackTrace();
            return new SliceResult(false, "Erro ao processar o vídeo: " + e.getMessage(), null);
        }
    }
}
