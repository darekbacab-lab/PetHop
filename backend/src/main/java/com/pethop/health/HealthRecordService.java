package com.pethop.health;

import com.pethop.adoption.AdoptionRepository;
import com.pethop.common.exception.ApiException;
import com.pethop.pet.Pet;
import com.pethop.pet.PetRepository;
import com.pethop.security.SecurityUser;
import com.pethop.shelter.ShelterRepository;
import com.pethop.user.UserRole;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HealthRecordService {
    private final HealthRecordRepository records;private final PetRepository pets;private final ShelterRepository shelters;private final AdoptionRepository adoptions;
    public HealthRecordService(HealthRecordRepository records,PetRepository pets,ShelterRepository shelters,AdoptionRepository adoptions){this.records=records;this.pets=pets;this.shelters=shelters;this.adoptions=adoptions;}
    @Transactional(readOnly=true) public List<com.pethop.health.dto.HealthRecordResponse> list(SecurityUser user,UUID petId){
        Pet pet=getPet(petId);authorize(user,pet,false);return records.findAllByPetIdOrderByPerformedAtDesc(petId).stream().map(com.pethop.health.dto.HealthRecordResponse::from).toList();
    }
    @Transactional(readOnly=true) public List<com.pethop.health.dto.HealthRecordResponse> upcoming(SecurityUser user,UUID petId){
        Pet pet=getPet(petId);authorize(user,pet,false);return records.findAllByPetIdAndNextDueAtGreaterThanEqualOrderByNextDueAtAsc(petId,LocalDate.now()).stream().map(com.pethop.health.dto.HealthRecordResponse::from).toList();
    }
    @Transactional public com.pethop.health.dto.HealthRecordResponse create(SecurityUser user,UUID petId,com.pethop.health.dto.HealthRecordRequest request){
        Pet pet=getPet(petId);authorize(user,pet,true);PetHealthRecord record=new PetHealthRecord();record.setPet(pet);record.setCreatedBy(user.user());record.setRecordType(request.recordType());record.setTitle(request.title().trim());record.setDescription(request.description());record.setPerformedAt(request.performedAt());record.setNextDueAt(request.nextDueAt());record.setVeterinarianName(request.veterinarianName());return com.pethop.health.dto.HealthRecordResponse.from(records.save(record));
    }
    private Pet getPet(UUID id){return pets.findByIdAndDeletedAtIsNull(id).orElseThrow(()->ApiException.notFound("Mascota no encontrada."));}
    private void authorize(SecurityUser user,Pet pet,boolean write){
        boolean owner=user.user().getRole()==UserRole.ADMIN||shelters.findByUserId(user.id()).map(s->s.getId().equals(pet.getShelter().getId())).orElse(false);
        if(write&&!owner)throw ApiException.forbidden("Solo el refugio propietario puede agregar datos al expediente.");
        if(!owner&&!adoptions.existsByPetIdAndAdopterId(pet.getId(),user.id()))throw ApiException.notFound("Expediente no encontrado.");
    }
}
