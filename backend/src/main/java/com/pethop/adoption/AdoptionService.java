package com.pethop.adoption;

import com.pethop.common.exception.ApiException;
import com.pethop.common.response.PageResponse;
import com.pethop.adoption.dto.AdoptionResponse;
import com.pethop.notification.NotificationService;
import com.pethop.pet.AdoptionStatus;
import com.pethop.pet.Pet;
import com.pethop.pet.PetRepository;
import com.pethop.security.SecurityUser;
import com.pethop.shelter.ShelterProfile;
import com.pethop.shelter.ShelterRepository;
import com.pethop.user.UserRole;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdoptionService {
    private final AdoptionRepository adoptions; private final ApplicationRepository applications;
    private final PetRepository pets; private final ShelterRepository shelters; private final NotificationService notifications;
    public AdoptionService(AdoptionRepository adoptions,ApplicationRepository applications,PetRepository pets,ShelterRepository shelters,NotificationService notifications){this.adoptions=adoptions;this.applications=applications;this.pets=pets;this.shelters=shelters;this.notifications=notifications;}
    @Transactional
    public AdoptionResponse create(SecurityUser principal,com.pethop.adoption.dto.CreateAdoptionRequest request){
        AdoptionApplication app=applications.findByIdForUpdate(request.applicationId()).orElseThrow(()->ApiException.notFound("Solicitud no encontrada."));
        if(principal.user().getRole()!=UserRole.ADMIN&&!isOwner(principal,app.getShelter()))throw ApiException.notFound("Solicitud no encontrada.");
        if(app.getStatus()!=ApplicationStatus.APPROVED)throw ApiException.conflict("Solo se puede formalizar una solicitud aprobada.");
        Pet pet=pets.findByIdForUpdate(app.getPet().getId()).orElseThrow(()->ApiException.notFound("Mascota no encontrada."));
        if(pet.getAdoptionStatus()!=AdoptionStatus.AVAILABLE&&pet.getAdoptionStatus()!=AdoptionStatus.IN_PROCESS)throw ApiException.conflict("La mascota ya no puede formalizar otra adopción.");
        if(adoptions.existsByPetId(pet.getId()))throw ApiException.conflict("La mascota ya tiene una adopción registrada.");
        Adoption adoption=new Adoption();adoption.setApplication(app);adoption.setPet(pet);adoption.setAdopter(app.getAdopter());adoption.setShelter(app.getShelter());
        adoption.setAdoptionDate(request.adoptionDate());adoption.setAgreementNotes(request.agreementNotes());
        adoption=adoptions.save(adoption);pet.setAdoptionStatus(AdoptionStatus.ADOPTED);pets.save(pet);
        notifications.create(app.getAdopter(),"ADOPTION_FORMALIZED","Adopción formalizada","La adopción de "+pet.getName()+" quedó registrada.","ADOPTION",adoption.getId());
        return AdoptionResponse.from(adoption);
    }
    @Transactional(readOnly=true) public PageResponse<AdoptionResponse> mine(SecurityUser user,int page,int size){return PageResponse.from(adoptions.findAllByAdopterIdOrderByAdoptionDateDesc(user.id(),PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"adoptionDate"))),AdoptionResponse::from);}
    @Transactional(readOnly=true) public PageResponse<AdoptionResponse> shelter(SecurityUser user,int page,int size){ShelterProfile shelter=shelters.findByUserId(user.id()).orElseThrow(()->ApiException.notFound("Perfil de refugio no encontrado."));return PageResponse.from(adoptions.findAllByShelterIdOrderByAdoptionDateDesc(shelter.getId(),PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"adoptionDate"))),AdoptionResponse::from);}
    @Transactional(readOnly=true) public Adoption authorized(UUID id,SecurityUser user){Adoption a=adoptions.findById(id).orElseThrow(()->ApiException.notFound("Adopción no encontrada."));if(user.user().getRole()!=UserRole.ADMIN&&!a.getAdopter().getId().equals(user.id())&&!isOwner(user,a.getShelter()))throw ApiException.notFound("Adopción no encontrada.");return a;}
    private boolean isOwner(SecurityUser user,ShelterProfile shelter){return user.user().getRole()==UserRole.SHELTER&&shelters.findByUserId(user.id()).map(s->s.getId().equals(shelter.getId())).orElse(false);}
}
