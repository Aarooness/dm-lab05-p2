package com.marketingfacil.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "campaigns")
public class Campaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String companyName;

    @Column(nullable = false, length = 120)
    private String productName;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, length = 180)
    private String originalImage;

    @Column(length = 180)
    private String squareImage;

    @Column(length = 180)
    private String verticalImage;

    @Column(length = 180)
    private String horizontalImage;

    @Column(length = 180)
    private String videoFile;

    private Long selectedScriptId;

    @Column(nullable = false, length = 20)
    private String generationStatus = "PENDING";

    @Column(length = 500)
    private String generationError;

    @Column(nullable = false, length = 30)
    private String publicationStatus = "PENDING_CONFIGURATION";

    private Instant simulatedAt;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<AdScript> scripts = new ArrayList<>();

    protected Campaign() {
    }

    public Campaign(String companyName, String productName, String description, String originalImage) {
        this.companyName = companyName;
        this.productName = productName;
        this.description = description;
        this.originalImage = originalImage;
    }

    public void addScript(AdScript script) {
        scripts.add(script);
    }

    public void chooseScript(Long scriptId) {
        selectedScriptId = scriptId;
        squareImage = null;
        verticalImage = null;
        horizontalImage = null;
        videoFile = null;
        generationStatus = "PENDING";
        generationError = null;
        updatedAt = Instant.now();
    }

    public void startGeneration() {
        if (selectedScriptId == null) {
            throw new IllegalStateException("Elige un guion antes de generar los materiales.");
        }
        if ("GENERATING".equals(generationStatus)) {
            throw new IllegalStateException("La generación de esta campaña ya está en curso.");
        }
        generationStatus = "GENERATING";
        generationError = null;
        updatedAt = Instant.now();
    }

    public void markReady(String squareImage, String verticalImage, String horizontalImage, String videoFile) {
        this.squareImage = squareImage;
        this.verticalImage = verticalImage;
        this.horizontalImage = horizontalImage;
        this.videoFile = videoFile;
        generationStatus = "READY";
        generationError = null;
        updatedAt = Instant.now();
    }

    public void markFailed(String message) {
        generationStatus = "ERROR";
        generationError = message;
        updatedAt = Instant.now();
    }

    public void markSimulated() {
        publicationStatus = "SIMULATED";
        simulatedAt = Instant.now();
        updatedAt = simulatedAt;
    }

    public Long getId() { return id; }
    public String getCompanyName() { return companyName; }
    public String getProductName() { return productName; }
    public String getDescription() { return description; }
    public String getOriginalImage() { return originalImage; }
    public String getSquareImage() { return squareImage; }
    public String getVerticalImage() { return verticalImage; }
    public String getHorizontalImage() { return horizontalImage; }
    public String getVideoFile() { return videoFile; }
    public Long getSelectedScriptId() { return selectedScriptId; }
    public String getGenerationStatus() { return generationStatus; }
    public String getGenerationError() { return generationError; }
    public String getPublicationStatus() { return publicationStatus; }
    public Instant getSimulatedAt() { return simulatedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<AdScript> getScripts() { return scripts; }
}
