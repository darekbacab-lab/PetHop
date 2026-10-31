package com.pethop.messaging;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ConversationRepository extends JpaRepository<Conversation,UUID>{Optional<Conversation> findByApplicationId(UUID applicationId);List<Conversation> findAllByPetId(UUID petId);}
