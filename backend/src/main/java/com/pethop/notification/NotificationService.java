package com.pethop.notification;

import com.pethop.common.exception.ApiException;
import com.pethop.common.response.PageResponse;
import com.pethop.security.SecurityUser;
import com.pethop.user.User;
import com.pethop.notification.dto.NotificationResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    public NotificationService(NotificationRepository notifications){this.notifications=notifications;}
    @Transactional public void create(User user,String type,String title,String message,String referenceType,UUID referenceId){
        Notification n=new Notification();n.setUser(user);n.setType(type);n.setTitle(title);n.setMessage(message);n.setReferenceType(referenceType);n.setReferenceId(referenceId);notifications.save(n);
    }
    @Transactional(readOnly=true)
    public PageResponse<NotificationResponse> list(SecurityUser user,int page,int size){
        return PageResponse.from(notifications.findAllByUserIdOrderByCreatedAtDesc(user.id(),PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt"))),NotificationResponse::from);
    }
    @Transactional public void markRead(SecurityUser user,UUID id){
        Notification n=notifications.findByIdAndUserId(id,user.id()).orElseThrow(()->ApiException.notFound("Notificación no encontrada."));
        if(n.getReadAt()==null)n.setReadAt(Instant.now());notifications.save(n);
    }
}
