package com.pethop.affiliate;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AffiliateRepository extends JpaRepository<Affiliate, UUID> {
    List<Affiliate> findAllByActiveTrueOrderByCategoryAscNameAsc();
    List<Affiliate> findAllByOrderByCategoryAscNameAsc();
}
