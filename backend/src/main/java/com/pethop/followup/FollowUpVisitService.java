package com.pethop.followup;

import com.pethop.adoption.Adoption;
import com.pethop.adoption.AdoptionService;
import com.pethop.common.exception.ApiException;
import com.pethop.followup.dto.FollowUpVisitRequest;
import com.pethop.followup.dto.FollowUpVisitResponse;
import com.pethop.security.SecurityUser;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowUpVisitService {
    private final FollowUpVisitRepository visits; private final AdoptionService adoptions;
    public FollowUpVisitService(FollowUpVisitRepository visits,AdoptionService adoptions){this.visits=visits;this.adoptions=adoptions;}
    @Transactional public FollowUpVisitResponse create(SecurityUser user,UUID adoptionId,FollowUpVisitRequest request){
        Adoption adoption=adoptions.authorized(adoptionId,user); validate(request);
        FollowUpVisit visit=new FollowUpVisit();visit.setAdoption(adoption);visit.setCreatedBy(user.user());apply(visit,request);return FollowUpVisitResponse.from(visits.save(visit));
    }
    @Transactional(readOnly=true) public List<FollowUpVisitResponse> list(SecurityUser user,UUID adoptionId){adoptions.authorized(adoptionId,user);return visits.findAllByAdoptionIdOrderByVisitDateDesc(adoptionId).stream().map(FollowUpVisitResponse::from).toList();}
    @Transactional public FollowUpVisitResponse update(SecurityUser user,UUID id,FollowUpVisitRequest request){
        FollowUpVisit visit=visits.findById(id).orElseThrow(()->ApiException.notFound("Visita no encontrada."));adoptions.authorized(visit.getAdoption().getId(),user);validate(request);apply(visit,request);return FollowUpVisitResponse.from(visits.save(visit));
    }
    private void validate(FollowUpVisitRequest request){if(request.status()==VisitStatus.COMPLETED&&request.visitDate().isAfter(LocalDate.now()))throw ApiException.badRequest("Una visita completada no puede tener una fecha futura.");if(request.status()==VisitStatus.CANCELLED&&request.outcome()!=null&&request.outcome().length()>120)throw ApiException.badRequest("El resultado de la visita es demasiado largo.");}
    private void apply(FollowUpVisit v,FollowUpVisitRequest q){v.setVisitDate(q.visitDate());v.setStatus(q.status());v.setOutcome(trim(q.outcome()));v.setNotes(trim(q.notes()));v.setNextAction(trim(q.nextAction()));}
    private String trim(String value){return value==null?null:value.trim();}
}
