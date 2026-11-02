package com.marketingfacil.service;

import com.marketingfacil.domain.AdScript;
import com.marketingfacil.domain.Campaign;
import com.marketingfacil.repository.CampaignRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampaignService {
    private final CampaignRepository campaigns;
    private final ScriptTemplateService templates;

    public CampaignService(CampaignRepository campaigns, ScriptTemplateService templates) {
        this.campaigns = campaigns;
        this.templates = templates;
    }

    @Transactional
    public Campaign create(String companyName, String productName, String description, String originalImage) {
        Campaign campaign = new Campaign(companyName.trim(), productName.trim(), description.trim(), originalImage);
        templates.createScripts(campaign).forEach(campaign::addScript);
        return campaigns.save(campaign);
    }

    @Transactional(readOnly = true)
    public List<Campaign> recentCampaigns() {
        return campaigns.findTop20ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Campaign find(Long id) {
        return campaigns.findWithScriptsById(id).orElseThrow(() -> new NoSuchElementException("No se encontró la campaña."));
    }

    @Transactional
    public AdScript chooseScript(Long campaignId, Long scriptId) {
        Campaign campaign = getCampaign(campaignId);
        if ("GENERATING".equals(campaign.getGenerationStatus())) {
            throw new IllegalStateException("No puedes cambiar el guion mientras se generan los materiales.");
        }
        AdScript selected = campaign.getScripts().stream()
                .filter(script -> script.getId().equals(scriptId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El guion seleccionado no pertenece a esta campaña."));
        campaign.chooseScript(scriptId);
        return selected;
    }

    @Transactional
    public void startGeneration(Long campaignId) {
        getCampaign(campaignId).startGeneration();
    }

    @Transactional(readOnly = true)
    public GenerationData getGenerationData(Long campaignId) {
        Campaign campaign = getCampaign(campaignId);
        AdScript script = campaign.getScripts().stream()
                .filter(candidate -> candidate.getId().equals(campaign.getSelectedScriptId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("La campaña no tiene un guion seleccionado."));
        return new GenerationData(campaign.getId(), campaign.getCompanyName(), campaign.getProductName(),
                campaign.getDescription(), campaign.getOriginalImage(),
                script.getTitle(), script.getBody(), script.getCallToAction());
    }

    @Transactional
    public void markReady(Long campaignId, String square, String vertical, String horizontal, String video) {
        getCampaign(campaignId).markReady(square, vertical, horizontal, video);
    }

    @Transactional
    public void markFailed(Long campaignId, String error) {
        getCampaign(campaignId).markFailed(error);
    }

    @Transactional
    public void simulatePublication(Long campaignId) {
        Campaign campaign = getCampaign(campaignId);
        if (!"READY".equals(campaign.getGenerationStatus())) {
            throw new IllegalStateException("Genera primero los materiales antes de simular la publicación.");
        }
        campaign.markSimulated();
    }

    private Campaign getCampaign(Long id) {
        return campaigns.findWithScriptsById(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontró la campaña."));
    }

    public record GenerationData(Long id, String companyName, String productName, String description,
                                 String originalImage, String title, String body, String callToAction) {
    }
}
