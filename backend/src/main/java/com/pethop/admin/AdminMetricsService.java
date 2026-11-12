package com.pethop.admin;

import com.pethop.admin.dto.AdminMetricsResponse;
import com.pethop.adoption.AdoptionRepository;
import com.pethop.adoption.ApplicationRepository;
import com.pethop.adoption.ApplicationStatus;
import com.pethop.pet.AdoptionStatus;
import com.pethop.pet.ModerationStatus;
import com.pethop.pet.PetRepository;
import com.pethop.shelter.ShelterRepository;
import com.pethop.shelter.VerificationStatus;
import com.pethop.user.AccountStatus;
import com.pethop.user.UserRepository;
import com.pethop.user.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminMetricsService {
    private final PetRepository pets; private final ApplicationRepository applications;
    private final AdoptionRepository adoptions; private final ShelterRepository shelters; private final UserRepository users;
    public AdminMetricsService(PetRepository pets, ApplicationRepository applications, AdoptionRepository adoptions,
            ShelterRepository shelters, UserRepository users) { this.pets=pets; this.applications=applications; this.adoptions=adoptions; this.shelters=shelters; this.users=users; }
    @Transactional(readOnly = true)
    public AdminMetricsResponse get() { return new AdminMetricsResponse(
            pets.countByDeletedAtIsNull(), pets.countByDeletedAtIsNullAndModerationStatus(ModerationStatus.PENDING),
            pets.countByDeletedAtIsNullAndAdoptionStatus(AdoptionStatus.AVAILABLE), applications.count(),
            applications.countByStatus(ApplicationStatus.SUBMITTED)+applications.countByStatus(ApplicationStatus.UNDER_REVIEW),
            adoptions.count(), shelters.countByVerificationStatus(VerificationStatus.PENDING),
            users.countByRoleAndAccountStatus(UserRole.ADOPTER, AccountStatus.ACTIVE), users.countByRoleAndAccountStatus(UserRole.SHELTER, AccountStatus.ACTIVE)); }
}
