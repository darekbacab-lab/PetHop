package com.pethop.shelter;

import com.pethop.common.exception.ApiException;
import com.pethop.shelter.dto.ShelterRequest;
import com.pethop.user.User;
import com.pethop.user.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShelterService {
    private final ShelterRepository shelters;
    public ShelterService(ShelterRepository shelters) { this.shelters = shelters; }
    @Transactional
    public ShelterProfile create(User user, ShelterRequest request) {
        if (user.getRole() != UserRole.SHELTER) throw ApiException.forbidden("Solo una cuenta de refugio puede crear este perfil.");
        if (shelters.findByUserId(user.getId()).isPresent()) throw ApiException.conflict("La cuenta ya tiene un perfil de refugio.");
        ShelterProfile profile = new ShelterProfile();
        profile.setUser(user);
        apply(profile, request);
        return shelters.save(profile);
    }
    @Transactional(readOnly = true)
    public ShelterProfile mine(User user) {
        return shelters.findByUserId(user.getId()).orElseThrow(() -> ApiException.notFound("No hay un perfil de refugio asociado a esta cuenta."));
    }
    @Transactional
    public ShelterProfile update(User user, ShelterRequest request) {
        ShelterProfile profile = mine(user);
        apply(profile, request);
        return shelters.save(profile);
    }
    private void apply(ShelterProfile profile, ShelterRequest request) {
        profile.setOrganizationName(request.organizationName().trim());
        profile.setDescription(request.description()); profile.setAddress(request.address());
        profile.setCity(request.city()); profile.setState(request.state()); profile.setPostalCode(request.postalCode());
        profile.setContactPhone(request.contactPhone());
    }
}
