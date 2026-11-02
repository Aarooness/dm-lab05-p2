package com.marketingfacil.service;

import com.marketingfacil.domain.AdScript;
import com.marketingfacil.domain.Campaign;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ScriptTemplateService {
    public List<AdScript> createScripts(Campaign campaign) {
        String product = campaign.getProductName();
        String company = campaign.getCompanyName();
        String description = campaign.getDescription().trim();

        return List.of(
                new AdScript(campaign, "Informativo", product + ": información clara",
                        description, "Conoce más sobre " + product),
                new AdScript(campaign, "Emocional", "Un momento para descubrir " + product,
                        company + " presenta " + product + ". " + description,
                        "Descubre " + product),
                new AdScript(campaign, "Promocional", "Presentamos " + product,
                        "Conoce " + product + " de " + company + ". " + description,
                        "Comunícate con " + company));
    }
}
