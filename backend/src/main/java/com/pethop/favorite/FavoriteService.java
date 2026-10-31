package com.pethop.favorite;

import com.pethop.common.exception.ApiException;
import com.pethop.pet.AdoptionStatus;
import com.pethop.pet.PetImageRepository;
import com.pethop.pet.PetRepository;
import com.pethop.pet.dto.PetImageResponse;
import com.pethop.pet.dto.PetResponse;
import com.pethop.security.SecurityUser;
import com.pethop.user.UserRole;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteService {
    private final FavoriteRepository favorites;private final PetRepository pets;private final PetImageRepository images;
    public FavoriteService(FavoriteRepository favorites,PetRepository pets,PetImageRepository images){this.favorites=favorites;this.pets=pets;this.images=images;}
    @Transactional(readOnly=true)
    public List<PetResponse> list(SecurityUser user){requireAdopter(user);return favorites.findAllByUserIdOrderByCreatedAtDesc(user.id()).stream().filter(f->f.getPet().getDeletedAt()==null&&f.getPet().getAdoptionStatus()==AdoptionStatus.AVAILABLE).map(f->PetResponse.from(f.getPet(),images.findAllByPetIdOrderByDisplayOrder(f.getPet().getId()).stream().map(PetImageResponse::from).toList())).toList();}
    @Transactional
    public PetResponse add(SecurityUser user,UUID petId){
        requireAdopter(user);var pet=pets.findByIdAndDeletedAtIsNull(petId).orElseThrow(()->ApiException.notFound("Mascota no encontrada."));
        if(pet.getAdoptionStatus()!=AdoptionStatus.AVAILABLE)throw ApiException.notFound("Mascota no encontrada.");
        if(favorites.existsByUserIdAndPetId(user.id(),petId))throw ApiException.conflict("La mascota ya está en favoritos.");
        Favorite favorite=new Favorite();favorite.setUser(user.user());favorite.setPet(pet);favorites.save(favorite);
        return PetResponse.from(pet,images.findAllByPetIdOrderByDisplayOrder(petId).stream().map(PetImageResponse::from).toList());
    }
    @Transactional public void remove(SecurityUser user,UUID petId){requireAdopter(user);favorites.findByUserIdAndPetId(user.id(),petId).ifPresent(favorites::delete);}
    private void requireAdopter(SecurityUser user){if(user.user().getRole()!=UserRole.ADOPTER)throw ApiException.forbidden("Solo las cuentas adoptantes pueden usar favoritos.");}
}
