package com.pethop.favorite;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FavoriteRepository extends JpaRepository<Favorite,UUID>{
    @EntityGraph(attributePaths = {"pet", "pet.shelter", "pet.personalityTraits"})
    List<Favorite> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Favorite> findByUserIdAndPetId(UUID userId,UUID petId);
    boolean existsByUserIdAndPetId(UUID userId,UUID petId);
}
