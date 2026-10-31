package com.pethop.adoption.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record AnswerRequest(@NotBlank @Size(max=100) String questionKey,@NotBlank @Size(max=500) String questionText,@NotBlank @Size(max=3000) String answerText) { }
