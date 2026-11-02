package com.marketingfacil.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ad_scripts")
public class AdScript {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false, length = 30)
    private String style;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(nullable = false, length = 700)
    private String body;

    @Column(nullable = false, length = 140)
    private String callToAction;

    protected AdScript() {
    }

    public AdScript(Campaign campaign, String style, String title, String body, String callToAction) {
        this.campaign = campaign;
        this.style = style;
        this.title = title;
        this.body = body;
        this.callToAction = callToAction;
    }

    public Long getId() { return id; }
    public Campaign getCampaign() { return campaign; }
    public String getStyle() { return style; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getCallToAction() { return callToAction; }
}
