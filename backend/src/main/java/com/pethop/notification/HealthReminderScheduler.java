package com.pethop.notification;

import com.pethop.adoption.Adoption;
import com.pethop.adoption.AdoptionRepository;
import com.pethop.health.HealthRecordRepository;
import com.pethop.health.PetHealthRecord;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class HealthReminderScheduler {
    private final HealthRecordRepository records;
    private final AdoptionRepository adoptions;
    private final NotificationService notifications;

    public HealthReminderScheduler(HealthRecordRepository records, AdoptionRepository adoptions, NotificationService notifications) {
        this.records = records; this.adoptions = adoptions; this.notifications = notifications;
    }

    @Scheduled(cron = "0 15 8 * * *")
    @Transactional
    public void createUpcomingReminders() {
        LocalDate today = LocalDate.now();
        records.findAllByNextDueAtBetween(today, today.plusDays(7)).forEach(record -> {
            Optional<Adoption> adoption = adoptions.findByPetId(record.getPet().getId());
            adoption.ifPresent(value -> {
                long days = Math.max(0, ChronoUnit.DAYS.between(today, record.getNextDueAt()));
                String when = days == 0 ? "hoy" : "en " + days + " días";
                notifications.create(value.getAdopter(), "HEALTH_REMINDER", "Próximo cuidado de " + record.getPet().getName(),
                        record.getTitle() + " está programado para " + when + ". Consulta a tu veterinario si necesitas orientación.",
                        "HEALTH_RECORD", record.getId(), "health-reminder:" + record.getId() + ":" + record.getNextDueAt());
            });
        });
    }
}
