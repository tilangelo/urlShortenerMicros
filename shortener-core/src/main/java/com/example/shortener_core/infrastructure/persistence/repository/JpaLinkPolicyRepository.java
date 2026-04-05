package com.example.shortener_core.infrastructure.persistence.repository;

import com.example.shortener_core.infrastructure.persistence.entity.LinkPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaLinkPolicyRepository extends JpaRepository<LinkPolicyEntity, Long> {
    
    Optional<LinkPolicyEntity> findByShortcode(String shortcode);
    
    Optional<LinkPolicyEntity> findByLinkId(Long linkId);
    
    boolean existsByShortcode(String shortcode);
    
    boolean existsByLinkId(Long linkId);
    
    @Query("SELECT lp FROM LinkPolicyEntity lp WHERE lp.shortcode = :shortcode")
    Optional<LinkPolicyEntity> findPolicyByShortcode(@Param("shortcode") String shortcode);
    
    @Query("SELECT lp FROM LinkPolicyEntity lp WHERE lp.linkId = :linkId")
    Optional<LinkPolicyEntity> findPolicyByLinkId(@Param("linkId") Long linkId);
}
