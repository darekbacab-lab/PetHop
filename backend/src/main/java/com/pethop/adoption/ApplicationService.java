package com.pethop.adoption;

import com.pethop.adoption.dto.AnswerRequest;
import com.pethop.adoption.dto.ApplicationResponse;
import com.pethop.adoption.dto.ApplicationReviewResponse;
import com.pethop.adoption.dto.CreateApplicationRequest;
import com.pethop.adoption.dto.StatusHistoryResponse;
import com.pethop.common.exception.ApiException;
import com.pethop.common.response.PageResponse;
import com.pethop.notification.NotificationService;
import com.pethop.pet.AdoptionStatus;
import com.pethop.pet.Pet;
import com.pethop.pet.PetRepository;
import com.pethop.security.SecurityUser;
import com.pethop.shelter.ShelterProfile;
import com.pethop.shelter.ShelterRepository;
import com.pethop.user.UserRole;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {
    private static final List<ApplicationStatus> ACTIVE = List.of(ApplicationStatus.DRAFT,ApplicationStatus.SUBMITTED,ApplicationStatus.UNDER_REVIEW,ApplicationStatus.APPROVED);
    private final ApplicationRepository applications;
    private final ApplicationAnswerRepository answers;
    private final ApplicationStatusHistoryRepository history;
    private final PetRepository pets;
    private final ShelterRepository shelters;
    private final NotificationService notifications;
    public ApplicationService(ApplicationRepository applications,ApplicationAnswerRepository answers,
            ApplicationStatusHistoryRepository history,PetRepository pets,ShelterRepository shelters,NotificationService notifications){
        this.applications=applications;this.answers=answers;this.history=history;this.pets=pets;this.shelters=shelters;this.notifications=notifications;
    }
    @Transactional
    public ApplicationResponse create(SecurityUser principal,CreateApplicationRequest request){
        if(principal.user().getRole()!=UserRole.ADOPTER)throw ApiException.forbidden("Solo una cuenta adoptante puede enviar una solicitud.");
        Pet pet=pets.findByIdAndDeletedAtIsNull(request.petId()).orElseThrow(()->ApiException.notFound("Mascota no encontrada."));
        if(pet.getAdoptionStatus()!=AdoptionStatus.AVAILABLE)throw ApiException.conflict("La mascota ya no está disponible.");
        if(applications.existsByAdopterIdAndPetIdAndStatusIn(principal.id(),pet.getId(),ACTIVE))throw ApiException.conflict("Ya tienes una solicitud activa para esta mascota.");
        AdoptionApplication application=new AdoptionApplication();application.setPet(pet);application.setAdopter(principal.user());
        application.setShelter(pet.getShelter());application.setStatus(ApplicationStatus.SUBMITTED);
        application.setMotivation(request.motivation().trim());application.setHousingType(request.housingType());
        application.setHasOtherPets(request.hasOtherPets());application.setHasChildren(request.hasChildren());
        application.setExperience(request.experience());application.setAvailability(request.availability());
        application.setAdditionalInformation(request.additionalInformation());
        application=applications.save(application);
        addHistory(application,null,ApplicationStatus.SUBMITTED,principal,"Solicitud enviada.");
        if(request.answers()!=null) for(AnswerRequest item:request.answers()) addAnswer(application,item);
        notifications.create(pet.getShelter().getUser(),"APPLICATION_SUBMITTED","Nueva solicitud de adopción",
                principal.user().getFullName()+" envió una solicitud para "+pet.getName()+".","APPLICATION",application.getId());
        return ApplicationResponse.from(application);
    }
    @Transactional(readOnly=true)
    public PageResponse<ApplicationResponse> mine(SecurityUser principal,int page,int size){
        return PageResponse.from(applications.findAllByAdopterIdOrderByCreatedAtDesc(principal.id(),pageable(page,size)),ApplicationResponse::from);
    }
    @Transactional(readOnly=true)
    public PageResponse<ApplicationReviewResponse> received(SecurityUser principal,int page,int size){
        ShelterProfile shelter=ownedShelter(principal);
        return PageResponse.from(applications.findAllByShelterIdOrderByCreatedAtDesc(shelter.getId(),pageable(page,size)),ApplicationReviewResponse::from);
    }
    @Transactional(readOnly=true)
    public ApplicationResponse get(SecurityUser principal,UUID id){
        AdoptionApplication application=applications.findById(id).orElseThrow(()->ApiException.notFound("Solicitud no encontrada."));
        if(!isAdopter(principal,application)&&!isShelterOwner(principal,application))throw ApiException.notFound("Solicitud no encontrada.");
        return ApplicationResponse.from(application);
    }
    @Transactional
    public void updateStatus(SecurityUser principal,UUID id,ApplicationStatus next,String comment){
        AdoptionApplication application=applications.findById(id).orElseThrow(()->ApiException.notFound("Solicitud no encontrada."));
        ApplicationStatus previous=application.getStatus();
        if(next==ApplicationStatus.WITHDRAWN){
            if(!isAdopter(principal,application))throw ApiException.notFound("Solicitud no encontrada.");
            if(previous!=ApplicationStatus.SUBMITTED&&previous!=ApplicationStatus.UNDER_REVIEW)throw ApiException.conflict("La solicitud ya no puede retirarse.");
        }else{
            if(!isShelterOwner(principal,application))throw ApiException.notFound("Solicitud no encontrada.");
            boolean valid=(previous==ApplicationStatus.SUBMITTED&&(next==ApplicationStatus.UNDER_REVIEW||next==ApplicationStatus.APPROVED||next==ApplicationStatus.REJECTED))
                    ||(previous==ApplicationStatus.UNDER_REVIEW&&(next==ApplicationStatus.APPROVED||next==ApplicationStatus.REJECTED));
            if(!valid)throw ApiException.conflict("El cambio de estado no está permitido.");
            application.setReviewedAt(Instant.now());application.setReviewedBy(principal.user());
            if(next==ApplicationStatus.APPROVED){application.getPet().setAdoptionStatus(AdoptionStatus.IN_PROCESS);pets.save(application.getPet());}
        }
        application.setStatus(next);applications.save(application);addHistory(application,previous,next,principal,comment);
        notifications.create(application.getAdopter(),"APPLICATION_STATUS_CHANGED","Actualización de solicitud",
                "El estado de tu solicitud para "+application.getPet().getName()+" cambió a "+next+".","APPLICATION",application.getId());
    }
    @Transactional(readOnly=true)
    public List<StatusHistoryResponse> history(SecurityUser principal,UUID id){
        AdoptionApplication app=applications.findById(id).orElseThrow(()->ApiException.notFound("Solicitud no encontrada."));
        if(!isAdopter(principal,app)&&!isShelterOwner(principal,app))throw ApiException.notFound("Solicitud no encontrada.");
        return history.findAllByApplicationIdOrderByChangedAtAsc(id).stream().map(StatusHistoryResponse::from).toList();
    }
    AdoptionApplication require(UUID id){return applications.findById(id).orElseThrow(()->ApiException.notFound("Solicitud no encontrada."));}
    private void addAnswer(AdoptionApplication application,AnswerRequest request){ApplicationAnswer answer=new ApplicationAnswer();answer.setApplication(application);answer.setQuestionKey(request.questionKey().trim());answer.setQuestionText(request.questionText().trim());answer.setAnswerText(request.answerText().trim());answers.save(answer);}
    private void addHistory(AdoptionApplication app,ApplicationStatus previous,ApplicationStatus next,SecurityUser actor,String comment){ApplicationStatusHistory item=new ApplicationStatusHistory();item.setApplication(app);item.setPreviousStatus(previous);item.setNewStatus(next);item.setChangedBy(actor.user());item.setComment(comment);history.save(item);}
    private ShelterProfile ownedShelter(SecurityUser principal){if(principal.user().getRole()!=UserRole.SHELTER)throw ApiException.forbidden("Solo un refugio puede consultar sus solicitudes recibidas.");return shelters.findByUserId(principal.id()).orElseThrow(()->ApiException.notFound("Perfil de refugio no encontrado."));}
    private boolean isAdopter(SecurityUser user,AdoptionApplication app){return user.user().getRole()==UserRole.ADOPTER&&app.getAdopter().getId().equals(user.id());}
    private boolean isShelterOwner(SecurityUser user,AdoptionApplication app){return user.user().getRole()==UserRole.ADMIN||(user.user().getRole()==UserRole.SHELTER&&shelters.findByUserId(user.id()).map(s->s.getId().equals(app.getShelter().getId())).orElse(false));}
    private org.springframework.data.domain.Pageable pageable(int page,int size){return PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt"));}
}
