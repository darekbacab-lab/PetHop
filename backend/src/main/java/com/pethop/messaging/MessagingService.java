package com.pethop.messaging;

import com.pethop.common.exception.ApiException;
import com.pethop.common.response.PageResponse;
import com.pethop.messaging.dto.ConversationResponse;
import com.pethop.messaging.dto.MessageResponse;
import com.pethop.notification.NotificationService;
import com.pethop.pet.AdoptionStatus;
import com.pethop.pet.Pet;
import com.pethop.pet.PetRepository;
import com.pethop.security.SecurityUser;
import com.pethop.user.User;
import com.pethop.user.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessagingService {
    private final ConversationRepository conversations;private final ConversationParticipantRepository participants;private final MessageRepository messages;
    private final PetRepository pets;private final NotificationService notifications;
    public MessagingService(ConversationRepository conversations,ConversationParticipantRepository participants,MessageRepository messages,PetRepository pets,NotificationService notifications){this.conversations=conversations;this.participants=participants;this.messages=messages;this.pets=pets;this.notifications=notifications;}
    @Transactional
    public ConversationResponse create(SecurityUser user,UUID petId){
        if(user.user().getRole()!=UserRole.ADOPTER)throw ApiException.forbidden("Solo una cuenta adoptante puede iniciar una conversación.");
        Pet pet=pets.findByIdAndDeletedAtIsNull(petId).filter(p->p.getAdoptionStatus()==AdoptionStatus.AVAILABLE||p.getAdoptionStatus()==AdoptionStatus.IN_PROCESS).orElseThrow(()->ApiException.notFound("Mascota no encontrada."));
        Conversation existing=findExistingPetThread(user.id(),petId,pet.getShelter().getUser().getId());
        if(existing!=null){
            ConversationParticipant participant=requireParticipantRecord(user,existing.getId());
            if(participant.getHiddenAt()!=null){participant.setHiddenAt(null);participants.save(participant);}
            return conversationResponse(existing,user.id(),participant.isPinned());
        }
        Conversation conversation=new Conversation();conversation.setPet(pet);conversation=conversations.save(conversation);
        addParticipant(conversation,user.user());addParticipant(conversation,pet.getShelter().getUser());
        return conversationResponse(conversation,user.id(),false);
    }
    @Transactional(readOnly=true)
    public List<ConversationResponse> list(SecurityUser user){
        return participants.findAllByUserIdAndHiddenAtIsNullOrderByPinnedDescJoinedAtDesc(user.id()).stream().map(p->conversationResponse(p.getConversation(),user.id(),p.isPinned())).toList();
    }
    @Transactional
    public void pin(SecurityUser user,UUID conversationId,boolean pinned){
        ConversationParticipant participant=requireParticipantRecord(user,conversationId);
        participant.setPinned(pinned);participants.save(participant);
    }
    @Transactional
    public void hide(SecurityUser user,UUID conversationId){
        ConversationParticipant participant=requireParticipantRecord(user,conversationId);
        participant.setHiddenAt(java.time.Instant.now());participants.save(participant);
    }
    @Transactional
    public PageResponse<MessageResponse> messages(SecurityUser user,UUID conversationId,int page,int size){
        requireParticipant(user,conversationId);messages.markUnreadAsRead(conversationId,user.id());
        return PageResponse.from(messages.findAllByConversationIdOrderByCreatedAtAsc(conversationId,PageRequest.of(page,size,Sort.by(Sort.Direction.ASC,"createdAt"))),MessageResponse::from);
    }
    @Transactional
    public MessageResponse send(SecurityUser user,UUID conversationId,String content){
        Conversation conversation=requireParticipant(user,conversationId);String normalized=content.trim();
        if(normalized.isEmpty())throw ApiException.badRequest("El mensaje no puede estar vacío.");
        Message message=new Message();message.setConversation(conversation);message.setSender(user.user());message.setContent(normalized);
        message=messages.save(message);conversation.touch();conversations.save(conversation);
        for(ConversationParticipant p:participants.findAllByConversationId(conversationId))if(!p.getUser().getId().equals(user.id()))
            notifications.create(p.getUser(),"NEW_MESSAGE","Nuevo mensaje","Tienes un mensaje nuevo sobre "+conversation.getPet().getName()+".","CONVERSATION",conversationId);
        return MessageResponse.from(message);
    }
    private Conversation requireParticipant(SecurityUser user,UUID id){
        if(!participants.existsByConversationIdAndUserId(id,user.id()))throw ApiException.notFound("Conversación no encontrada.");
        return conversations.findById(id).orElseThrow(()->ApiException.notFound("Conversación no encontrada."));
    }
    private ConversationParticipant requireParticipantRecord(SecurityUser user,UUID id){
        return participants.findByConversationIdAndUserId(id,user.id()).orElseThrow(()->ApiException.notFound("Conversación no encontrada."));
    }
    private Conversation findExistingPetThread(UUID adopterId,UUID petId,UUID shelterId){
        return conversations.findAllByPetId(petId).stream().filter(c->participants.existsByConversationIdAndUserId(c.getId(),adopterId)&&participants.existsByConversationIdAndUserId(c.getId(),shelterId)).findFirst().orElse(null);
    }
    private void addParticipant(Conversation conversation,User user){ConversationParticipant p=new ConversationParticipant();p.setConversation(conversation);p.setUser(user);participants.save(p);}
    private ConversationResponse conversationResponse(Conversation c,UUID currentUserId,boolean pinned){
        List<ConversationParticipant> people=participants.findAllByConversationId(c.getId());
        String other=people.stream().map(ConversationParticipant::getUser).filter(u->!u.getId().equals(currentUserId)).map(User::getFullName).findFirst().orElse("Participante");
        Optional<Message> last=messages.findFirstByConversationIdOrderByCreatedAtDesc(c.getId());
        long unread=messages.countByConversationIdAndReadAtIsNullAndSender_IdNot(c.getId(),currentUserId);
        return ConversationResponse.from(c,other,pinned,last.map(Message::getContent).orElse(null),unread);
    }
}
