package com.pethop.health.dto;
import com.pethop.health.HealthRecordType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
public record HealthRecordRequest(@NotNull HealthRecordType recordType,@NotBlank @Size(max=180) String title,@Size(max=4000) String description,@NotNull LocalDate performedAt,LocalDate nextDueAt,@Size(max=160) String veterinarianName){}
