package com.marketingfacil.repository;

import com.marketingfacil.domain.Campaign;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findTop20ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "scripts")
    Optional<Campaign> findWithScriptsById(Long id);
}
