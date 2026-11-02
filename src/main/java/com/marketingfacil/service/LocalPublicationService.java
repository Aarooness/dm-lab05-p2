package com.marketingfacil.service;

import org.springframework.stereotype.Service;

@Service
public class LocalPublicationService implements PublicationService {
    private final CampaignService campaigns;

    public LocalPublicationService(CampaignService campaigns) {
        this.campaigns = campaigns;
    }

    @Override
    public void simulate(Long campaignId) {
        campaigns.simulatePublication(campaignId);
    }
}
