package com.pethop.affiliate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AffiliateRequest(@NotBlank @Size(max=180) String name,@NotBlank @Size(max=40) String category,
        @Size(max=4000) String description,@Size(max=180) String location,@Size(max=500) String websiteUrl,
        @Size(max=80) String discountCode,@Size(max=240) String discountText,Boolean active) {}
