package com.pethop.followup;

import com.pethop.adoption.Adoption;
import com.pethop.adoption.AdoptionService;
import com.pethop.common.exception.ApiException;
import com.pethop.followup.dto.FollowUpMediaResponse;
import com.pethop.followup.dto.FollowUpRequest;
import com.pethop.followup.dto.FollowUpResponse;
import com.pethop.notification.NotificationService;
import com.pethop.pet.storage.FileStorage;
import com.pethop.pet.storage.StoredFile;
import com.pethop.security.SecurityUser;
import com.pethop.user.UserRole;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FollowUpService {
    private final FollowUpRepository reports;private final FollowUpMediaRepository media;private final AdoptionService adoptions;
    private final FileStorage files;private final NotificationService notifications;
    public FollowUpService(FollowUpRepository reports,FollowUpMediaRepository media,AdoptionService adoptions,FileStorage files,NotificationService notifications){this.reports=reports;this.media=media;this.adoptions=adoptions;this.files=files;this.notifications=notifications;}
    @Transactional
    public FollowUpResponse create(SecurityUser user,UUID adoptionId,FollowUpRequest request){
        Adoption adoption=adoptions.authorized(adoptionId,user);FollowUpReport report=new FollowUpReport();report.setAdoption(adoption);report.setCreatedBy(user.user());apply(report,request);report=reports.save(report);
        notifications.create(otherParty(adoption,user),"FOLLOW_UP_CREATED","Nuevo reporte de seguimiento","Se agregó un reporte para "+adoption.getPet().getName()+".","FOLLOW_UP",report.getId());
        return response(report);
    }
    @Transactional(readOnly=true)
    public List<FollowUpResponse> list(SecurityUser user,UUID adoptionId){
        adoptions.authorized(adoptionId,user);
        return reports.findAllByAdoptionIdOrderByReportDateDesc(adoptionId).stream().map(this::response).toList();
    }
    @Transactional
    public FollowUpResponse update(SecurityUser user,UUID reportId,FollowUpRequest request){
        FollowUpReport report=reports.findById(reportId).orElseThrow(()->ApiException.notFound("Reporte no encontrado."));
        adoptions.authorized(report.getAdoption().getId(),user);
        if(!report.getCreatedBy().getId().equals(user.id()))throw ApiException.forbidden("Solo quien creó el reporte puede editarlo.");
        apply(report,request);return response(reports.save(report));
    }
    @Transactional
    public FollowUpMediaResponse addMedia(SecurityUser user,UUID reportId,MultipartFile file)throws IOException{
        FollowUpReport report=reports.findById(reportId).orElseThrow(()->ApiException.notFound("Reporte no encontrado."));adoptions.authorized(report.getAdoption().getId(),user);
        if(!report.getCreatedBy().getId().equals(user.id()))throw ApiException.forbidden("Solo quien creó el reporte puede adjuntar fotografías.");
        if(media.countByFollowUpId(reportId)>=6)throw ApiException.badRequest("Cada reporte puede tener hasta 6 fotografías.");
        StoredFile stored=files.store(file);FollowUpMedia item=new FollowUpMedia();item.setFollowUp(report);item.setStoragePath(stored.path());item.setOriginalFilename(stored.originalFilename());item.setContentType(stored.contentType());item.setFileSize(stored.size());
        return FollowUpMediaResponse.from(media.save(item));
    }
    @Transactional(readOnly=true)
    public FollowUpMedia getMedia(SecurityUser user,UUID mediaId){
        FollowUpMedia item=media.findById(mediaId).orElseThrow(()->ApiException.notFound("Fotografía no encontrada."));adoptions.authorized(item.getFollowUp().getAdoption().getId(),user);return item;
    }
    private void apply(FollowUpReport r,FollowUpRequest q){r.setReportDate(q.reportDate());r.setPetCondition(q.petCondition());r.setBehaviorNotes(q.behaviorNotes());r.setHealthNotes(q.healthNotes());r.setGeneralNotes(q.generalNotes());}
    private FollowUpResponse response(FollowUpReport r){return FollowUpResponse.from(r,media.findAllByFollowUpId(r.getId()).stream().map(FollowUpMediaResponse::from).toList());}
    private com.pethop.user.User otherParty(Adoption a,SecurityUser actor){return actor.user().getRole()==UserRole.SHELTER?a.getAdopter():a.getShelter().getUser();}
}
