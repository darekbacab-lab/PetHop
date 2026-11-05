package com.pethop.veterinary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pethop.adoption.AdoptionRepository;
import com.pethop.common.exception.ApiException;
import com.pethop.pet.Pet;
import com.pethop.pet.PetRepository;
import com.pethop.security.SecurityUser;
import com.pethop.shelter.ShelterRepository;
import com.pethop.user.UserRole;
import com.pethop.veterinary.dto.VeterinaryAssistantResponse;
import com.pethop.veterinary.dto.VeterinaryPetContextResponse;
import java.text.Normalizer;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class VeterinaryAssistantService {
    private static final Logger log = LoggerFactory.getLogger(VeterinaryAssistantService.class);
    private static final String DISCLAIMER = "Orientación preliminar: no sustituye la evaluación de un veterinario.";
    private final PetRepository pets;
    private final ShelterRepository shelters;
    private final AdoptionRepository adoptions;
    private final VeterinaryAiProvider provider;
    private final ObjectMapper mapper;

    public VeterinaryAssistantService(PetRepository pets, ShelterRepository shelters, AdoptionRepository adoptions,
            VeterinaryAiProvider provider, ObjectMapper mapper) {
        this.pets = pets; this.shelters = shelters; this.adoptions = adoptions; this.provider = provider; this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public VeterinaryPetContextResponse context(SecurityUser user, UUID petId) {
        return toContext(authorizedPet(user, petId));
    }

    @Transactional(readOnly = true)
    public VeterinaryAssistantResponse ask(SecurityUser user, UUID petId, String message) {
        Pet pet = authorizedPet(user, petId);
        return askForPet(user, pet, message);
    }

    @Transactional(readOnly = true)
    public VeterinaryAssistantResponse askGeneral(SecurityUser user, UUID petId, String message) {
        Pet pet = petId == null ? null : authorizedPet(user, petId);
        return askForPet(user, pet, message);
    }

    private VeterinaryAssistantResponse askForPet(SecurityUser user, Pet pet, String message) {
        String normalizedMessage = message.trim();
        log.info("Veterinary assistant request started authenticated={} messageLength={}", user != null, normalizedMessage.length());
        if (isEmergency(normalizedMessage)) return emergencyResponse(pet);
        log.info("Veterinary assistant provider selected provider={}", provider.getClass().getSimpleName());
        String raw = provider.generate(prompt(pet == null ? null : toContext(pet), normalizedMessage));
        return parseResponse(pet, raw);
    }

    private Pet authorizedPet(SecurityUser user, UUID petId) {
        Pet pet = pets.findByIdAndDeletedAtIsNull(petId)
                .orElseThrow(() -> ApiException.notFound("Mascota no encontrada."));
        boolean publicPet = pet.getAdoptionStatus() == com.pethop.pet.AdoptionStatus.AVAILABLE
                && pet.getModerationStatus() == com.pethop.pet.ModerationStatus.APPROVED;
        boolean admin = user != null && user.user().getRole() == UserRole.ADMIN;
        boolean shelterOwner = user != null && user.user().getRole() == UserRole.SHELTER
                && shelters.findByUserId(user.id()).map(s -> s.getId().equals(pet.getShelter().getId())).orElse(false);
        boolean adopter = user != null && adoptions.existsByPetIdAndAdopterId(petId, user.id());
        if (!publicPet && !admin && !shelterOwner && !adopter)
            throw ApiException.notFound("Mascota no encontrada.");
        return pet;
    }

    private VeterinaryPetContextResponse toContext(Pet pet) {
        return new VeterinaryPetContextResponse(pet.getId(), pet.getName(), pet.getSpecies(), pet.getBreed(), pet.getSex(),
                pet.getAgeValue(), pet.getAgeUnit(), pet.getLifeStage(), pet.getSize(), pet.getWeight(), pet.getColor(),
                pet.getHealthSummary(), pet.getHealthCondition(), pet.getSterilized(), pet.getVaccinated(), pet.getDewormed(),
                pet.getConditions(), pet.getRequirements());
    }

    private String prompt(VeterinaryPetContextResponse pet, String message) {
        if (pet == null) return "No hay una mascota específica seleccionada. Proporciona orientación veterinaria general y preventiva.\n"
                + "Observación del usuario:\n" + message;
        return "Contexto autorizado de la mascota (sin datos personales):\n"
                + "nombre=" + value(pet.name()) + ", especie=" + value(pet.species()) + ", raza=" + value(pet.breed())
                + ", sexo=" + value(pet.sex()) + ", edad=" + value(pet.ageValue()) + " " + value(pet.ageUnit())
                + ", etapa=" + value(pet.lifeStage()) + ", tamaño=" + value(pet.size()) + ", peso=" + value(pet.weight())
                + ", color=" + value(pet.color()) + ", salud=" + value(pet.healthSummary())
                + ", condición=" + value(pet.healthCondition()) + ", esterilizado=" + value(pet.sterilized())
                + ", vacunado=" + value(pet.vaccinated()) + ", desparasitado=" + value(pet.dewormed())
                + ", condiciones del hogar=" + value(pet.conditions()) + ", requisitos=" + value(pet.requirements())
                + "\nObservación del usuario:\n" + message;
    }

    private VeterinaryAssistantResponse parseResponse(Pet pet, String raw) {
        try {
            String json = extractJson(raw);
            JsonNode root = mapper.readTree(json);
            VeterinaryUrgency urgency = VeterinaryUrgency.valueOf(root.path("urgency").asText().trim().toUpperCase(Locale.ROOT));
            String summary = required(root, "summary");
            List<String> reasons = list(root, "reasons");
            List<String> warningSigns = list(root, "warning_signs", "warningSigns");
            List<String> guidance = list(root, "guidance");
            return new VeterinaryAssistantResponse(pet == null ? null : pet.getId(), pet == null ? null : pet.getName(), urgency, summary, reasons, warningSigns, guidance, DISCLAIMER, Instant.now());
        } catch (ApiException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            log.warn("Veterinary assistant response parsing failed exception={} rawLength={}", exception.getClass().getName(), raw == null ? 0 : raw.length());
            throw ApiException.badGateway("El servicio veterinario devolvió una respuesta no válida.");
        }
    }

    private String required(JsonNode root, String field) {
        String value = root.path(field).asText("").trim();
        if (value.isBlank() || value.length() > 4000) throw ApiException.badGateway("El servicio veterinario devolvió una respuesta no válida.");
        return value;
    }

    private List<String> list(JsonNode root, String... fields) {
        JsonNode node = null;
        for (String field : fields) {
            if (root.has(field)) { node = root.path(field); break; }
        }
        if (node == null || !node.isArray() || node.size() > 8) throw ApiException.badGateway("El servicio veterinario devolvió una respuesta no válida.");
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            if (!item.isTextual() || item.asText().isBlank()) throw ApiException.badGateway("El servicio veterinario devolvió una respuesta no válida.");
            values.add(item.asText().trim());
        });
        return List.copyOf(values);
    }

    private String extractJson(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("```")) value = value.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end < start) throw ApiException.badGateway("El servicio veterinario devolvió una respuesta no válida.");
        return value.substring(start, end + 1).trim();
    }

    private boolean isEmergency(String message) {
        String value = Normalizer.normalize(message, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return value.matches(".*(dificultad para respirar|no puede respirar|convulsion|sangrado abundante|inconsciente|desmayo|envenen|ingirio veneno|trauma grave|dolor intenso|abdomen hinchado).*" );
    }

    private VeterinaryAssistantResponse emergencyResponse(Pet pet) {
        return new VeterinaryAssistantResponse(pet == null ? null : pet.getId(), pet == null ? null : pet.getName(), VeterinaryUrgency.RED,
                "Los signos descritos pueden requerir atención veterinaria urgente.",
                List.of("La dificultad respiratoria, la pérdida de conciencia, las convulsiones, el sangrado abundante o el dolor intenso son señales de alarma."),
                List.of("Dificultad para respirar, encías azuladas o muy pálidas, pérdida de conciencia, convulsiones, sangrado abundante o empeoramiento rápido."),
                List.of("Contacta ahora a un servicio veterinario de urgencias. Mantén a la mascota segura y tranquila durante el traslado. No administres medicamentos humanos ni esperes a que los signos desaparezcan."),
                DISCLAIMER, Instant.now());
    }

    private String value(Object value) { return value == null ? "no proporcionado" : String.valueOf(value); }
}
