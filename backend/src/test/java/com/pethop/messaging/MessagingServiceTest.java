package com.pethop.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.pethop.adoption.ApplicationRepository;
import com.pethop.messaging.dto.ConversationResponse;
import com.pethop.notification.NotificationService;
import com.pethop.pet.Pet;
import com.pethop.pet.PetRepository;
import com.pethop.security.SecurityUser;
import com.pethop.user.User;
import com.pethop.user.UserRole;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MessagingServiceTest {
    @Mock ConversationRepository conversations;
    @Mock ConversationParticipantRepository participants;
    @Mock MessageRepository messages;
    @Mock PetRepository pets;
    @Mock ApplicationRepository applications;
    @Mock NotificationService notifications;
    private MessagingService service;
    private User adopter;
    private User shelterUser;
    private Conversation conversation;

    @BeforeEach void setUp() {
        service = new MessagingService(conversations, participants, messages, pets, applications, notifications);
        adopter = new User();
        ReflectionTestUtils.setField(adopter, "id", UUID.randomUUID());
        adopter.setFullName("Adoptante");
        adopter.setRole(UserRole.ADOPTER);
        shelterUser = new User();
        ReflectionTestUtils.setField(shelterUser, "id", UUID.randomUUID());
        shelterUser.setFullName("Refugio Esperanza");
        shelterUser.setRole(UserRole.SHELTER);
        Pet pet = new Pet();
        ReflectionTestUtils.setField(pet, "id", UUID.randomUUID());
        pet.setName("Rita");
        conversation = new Conversation();
        ReflectionTestUtils.setField(conversation, "id", UUID.randomUUID());
        conversation.setPet(pet);
        ReflectionTestUtils.setField(conversation, "createdAt", Instant.now());
        ReflectionTestUtils.setField(conversation, "updatedAt", Instant.now());
    }

    private ConversationParticipant participant(User user) {
        ConversationParticipant participant = new ConversationParticipant();
        participant.setConversation(conversation);
        participant.setUser(user);
        return participant;
    }

    @Test void listShowsLastMessageAndUnreadCountForTheOtherSide() {
        Message last = new Message();
        last.setConversation(conversation);
        last.setSender(adopter);
        last.setContent("Hola, ¿sigue disponible?");
        ReflectionTestUtils.setField(last, "createdAt", Instant.now());
        lenient().when(participants.findAllByUserIdAndHiddenAtIsNullOrderByPinnedDescJoinedAtDesc(shelterUser.getId()))
                .thenReturn(List.of(participant(shelterUser)));
        lenient().when(participants.findAllByConversationId(conversation.getId()))
                .thenReturn(List.of(participant(adopter), participant(shelterUser)));
        lenient().when(messages.findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId()))
                .thenReturn(Optional.of(last));
        lenient().when(messages.countByConversationIdAndReadAtIsNullAndSender_IdNot(conversation.getId(), shelterUser.getId()))
                .thenReturn(1L);

        List<ConversationResponse> result = service.list(new SecurityUser(shelterUser));

        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.get(0).otherParticipant()).isEqualTo("Adoptante");
        Assertions.assertThat(result.get(0).lastMessage()).isEqualTo("Hola, ¿sigue disponible?");
        Assertions.assertThat(result.get(0).unreadCount()).isEqualTo(1L);
    }

    @Test void listShowsTheShelterAsContactForTheAdopter() {
        lenient().when(participants.findAllByUserIdAndHiddenAtIsNullOrderByPinnedDescJoinedAtDesc(adopter.getId()))
                .thenReturn(List.of(participant(adopter)));
        lenient().when(participants.findAllByConversationId(conversation.getId()))
                .thenReturn(List.of(participant(adopter), participant(shelterUser)));
        lenient().when(messages.findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId()))
                .thenReturn(Optional.empty());
        lenient().when(messages.countByConversationIdAndReadAtIsNullAndSender_IdNot(conversation.getId(), adopter.getId()))
                .thenReturn(0L);

        List<ConversationResponse> result = service.list(new SecurityUser(adopter));

        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.get(0).otherParticipant()).isEqualTo("Refugio Esperanza");
        Assertions.assertThat(result.get(0).lastMessage()).isNull();
        Assertions.assertThat(result.get(0).unreadCount()).isEqualTo(0L);
    }
}
