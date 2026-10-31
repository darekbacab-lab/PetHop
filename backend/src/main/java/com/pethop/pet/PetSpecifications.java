package com.pethop.pet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

public final class PetSpecifications {
    private PetSpecifications() { }
    public static Specification<Pet> filters(PetSpecies species, PetSex sex, PetSize size, String location, String search, AdoptionStatus status, java.util.UUID shelterId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (species != null) predicates.add(cb.equal(root.get("species"), species));
            if (sex != null) predicates.add(cb.equal(root.get("sex"), sex));
            if (size != null) predicates.add(cb.equal(root.get("size"), size));
            if (status != null) predicates.add(cb.equal(root.get("adoptionStatus"), status));
            if (shelterId != null) predicates.add(cb.equal(root.get("shelter").get("id"), shelterId));
            if (location != null && !location.isBlank()) predicates.add(cb.like(cb.lower(root.get("location")), "%" + location.trim().toLowerCase(Locale.ROOT) + "%"));
            if (search != null && !search.isBlank()) {
                String term = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("name")), term), cb.like(cb.lower(root.get("breed")), term), cb.like(cb.lower(root.get("description")), term)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
