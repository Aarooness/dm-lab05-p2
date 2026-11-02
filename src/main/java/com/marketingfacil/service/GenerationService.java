package com.marketingfacil.service;

import com.marketingfacil.domain.AdScript;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class GenerationService {
    private static final Logger logger = LoggerFactory.getLogger(GenerationService.class);
    private final CampaignService campaigns;
    private final FileStorageService files;
    private final ArtworkService artwork;
    private final VideoService video;
    private final ThreadPoolTaskExecutor executor;

    public GenerationService(CampaignService campaigns, FileStorageService files, ArtworkService artwork,
                             VideoService video, @Qualifier("generationExecutor") ThreadPoolTaskExecutor executor) {
        this.campaigns = campaigns;
        this.files = files;
        this.artwork = artwork;
        this.video = video;
        this.executor = executor;
    }

    public void queueGeneration(Long campaignId) {
        campaigns.startGeneration(campaignId);
        try {
            executor.execute(() -> generate(campaignId));
        } catch (RejectedExecutionException exception) {
            campaigns.markFailed(campaignId, "La cola de generación está llena. Inténtalo de nuevo en unos minutos.");
            throw new IllegalStateException("Hay demasiadas generaciones en curso. Inténtalo de nuevo en unos minutos.", exception);
        }
    }

    private void generate(Long campaignId) {
        List<Path> createdFiles = new ArrayList<>();
        try {
            CampaignService.GenerationData data = campaigns.getGenerationData(campaignId);
            AdScript selected = new AdScript(null, "seleccionado", data.title(), data.body(), data.callToAction());
            Path original = files.resolve(data.originalImage());

            String square = files.newOutputName("cuadrada.png");
            String vertical = files.newOutputName("vertical.png");
            String horizontal = files.newOutputName("horizontal.png");
            String videoName = files.newOutputName("video.mp4");
            Path squarePath = files.resolve(square);
            Path verticalPath = files.resolve(vertical);
            Path horizontalPath = files.resolve(horizontal);
            Path videoPath = files.resolve(videoName);
            createdFiles.addAll(List.of(squarePath, verticalPath, horizontalPath, videoPath));

            artwork.createDesign(original, squarePath,
                    new com.marketingfacil.domain.Campaign(data.companyName(), data.productName(), data.description(), data.originalImage()),
                    selected, 1080, 1080);
            artwork.createDesign(original, verticalPath,
                    new com.marketingfacil.domain.Campaign(data.companyName(), data.productName(), data.description(), data.originalImage()),
                    selected, 1080, 1350);
            artwork.createDesign(original, horizontalPath,
                    new com.marketingfacil.domain.Campaign(data.companyName(), data.productName(), data.description(), data.originalImage()),
                    selected, 1600, 900);
            video.createVideo(List.of(squarePath, verticalPath, horizontalPath), videoPath);
            campaigns.markReady(campaignId, square, vertical, horizontal, videoName);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            fail(campaignId, createdFiles, "La generación se interrumpió. Puedes volver a intentarlo.");
        } catch (IOException | RuntimeException exception) {
            logger.error("Error generando materiales para la campaña {}", campaignId, exception);
            fail(campaignId, createdFiles, "No se pudieron generar los materiales: " + safeMessage(exception));
        }
    }

    private void fail(Long campaignId, List<Path> createdFiles, String message) {
        for (Path path : createdFiles) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException exception) {
                logger.warn("No se pudo limpiar el archivo temporal de la campaña {}", campaignId, exception);
            }
        }
        campaigns.markFailed(campaignId, message);
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return "Error interno. Revisa los registros del servidor.";
        return message.length() > 400 ? message.substring(0, 400) : message;
    }
}
