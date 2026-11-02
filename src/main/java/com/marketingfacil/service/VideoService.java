package com.marketingfacil.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class VideoService {
    public void createVideo(List<Path> images, Path destination) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                "ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
                "-loop", "1", "-framerate", "25", "-t", "3", "-i", images.get(0).toString(),
                "-loop", "1", "-framerate", "25", "-t", "3", "-i", images.get(1).toString(),
                "-loop", "1", "-framerate", "25", "-t", "3", "-i", images.get(2).toString(),
                "-filter_complex",
                "[0:v]scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2,setsar=1[v0];" +
                        "[1:v]scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2,setsar=1[v1];" +
                        "[2:v]scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2,setsar=1[v2];" +
                        "[v0][v1][v2]concat=n=3:v=1:a=0,format=yuv420p[v]",
                "-map", "[v]", "-r", "25", "-t", "9", "-movflags", "+faststart", destination.toString());
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        Process process = builder.start();
        boolean completed;
        try {
            completed = process.waitFor(120, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw exception;
        }
        if (!completed) {
            process.destroyForcibly();
            throw new IOException("FFmpeg excedió el límite de 120 segundos.");
        }
        if (process.exitValue() != 0 || !java.nio.file.Files.isRegularFile(destination)) {
            throw new IOException("FFmpeg no pudo crear el video. Verifica los archivos generados e inténtalo de nuevo.");
        }
    }
}
