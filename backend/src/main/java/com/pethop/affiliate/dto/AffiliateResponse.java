package com.pethop.affiliate.dto;

import com.pethop.affiliate.Affiliate;
import java.time.Instant;
import java.util.UUID;

public record AffiliateResponse(UUID id,String name,String category,String description,String location,String websiteUrl,
        String discountCode,String discountText,boolean active,Instant createdAt,Instant updatedAt) {
    public static AffiliateResponse from(Affiliate a){return new AffiliateResponse(a.getId(),a.getName(),a.getCategory(),a.getDescription(),a.getLocation(),a.getWebsiteUrl(),a.getDiscountCode(),a.getDiscountText(),a.isActive(),a.getCreatedAt(),a.getUpdatedAt());}
}
