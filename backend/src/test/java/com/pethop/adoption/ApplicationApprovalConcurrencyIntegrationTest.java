package com.pethop.adoption;

import static org.assertj.core.api.Assertions.assertThat;

import com.pethop.common.exception.ApiException;
import com.pethop.pet.AdoptionStatus;
import com.pethop.security.SecurityUser;
import com.pethop.user.User;
import com.pethop.user.UserRole;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class ApplicationApprovalConcurrencyIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired ApplicationService applications;
    @Autowired TransactionTemplate transactions;

    private UUID shelterUserId;
    private UUID shelterId;
    private UUID petId;
    private SecurityUser shelterPrincipal;
    private UUID firstApplicationId;
    private UUID secondApplicationId;

    @BeforeEach
    void setUp() {
        shelterUserId = UUID.randomUUID();
        shelterId = UUID.randomUUID();
        petId = UUID.randomUUID();
        firstApplicationId = UUID.randomUUID();
        secondApplicationId = UUID.randomUUID();
        UUID firstAdopterId = UUID.randomUUID();
        UUID secondAdopterId = UUID.randomUUID();

        insertUser(shelterUserId, "Shelter concurrency", UserRole.SHELTER);
        insertUser(firstAdopterId, "First adopter", UserRole.ADOPTER);
        insertUser(secondAdopterId, "Second adopter", UserRole.ADOPTER);
        jdbc.update("insert into shelter_profiles(id, user_id, organization_name, verification_status) values (?, ?, ?, 'VERIFIED')",
                shelterId, shelterUserId, "Concurrent Shelter");
        jdbc.update("insert into pets(id, shelter_id, name, species, sex, description, adoption_status) "
                        + "values (?, ?, 'Nube', 'CAT', 'FEMALE', 'Busca un hogar.', 'AVAILABLE')",
                petId, shelterId);
        insertApplication(firstApplicationId, firstAdopterId);
        insertApplication(secondApplicationId, secondAdopterId);

        User shelter = new User();
        setId(shelter, shelterUserId);
        shelter.setRole(UserRole.SHELTER);
        shelter.setFullName("Shelter concurrency");
        shelter.setEmail("shelter-" + shelterUserId + "@example.test");
        shelterPrincipal = new SecurityUser(shelter);
    }

    @Test
    void concurrentApprovalsUseThePetRowLockAndOnlyOneCommits() throws Exception {
        CyclicBarrier bothTransactionsReady = new CyclicBarrier(2);
        AtomicInteger transactionsEntered = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<ApprovalAttempt> first = executor.submit(() -> approveConcurrently(firstApplicationId,
                    bothTransactionsReady, transactionsEntered));
            Future<ApprovalAttempt> second = executor.submit(() -> approveConcurrently(secondApplicationId,
                    bothTransactionsReady, transactionsEntered));

            ApprovalAttempt firstResult = first.get(30, TimeUnit.SECONDS);
            ApprovalAttempt secondResult = second.get(30, TimeUnit.SECONDS);

            assertThat(transactionsEntered).hasValue(2);
            assertThat(firstResult.succeeded() ^ secondResult.succeeded()).isTrue();
            ApprovalAttempt loser = firstResult.succeeded() ? secondResult : firstResult;
            assertThat(loser.failure()).isInstanceOf(ApiException.class)
                    .hasMessage("La mascota ya no está disponible para aprobación.");

            assertThat(jdbc.queryForObject("select adoption_status from pets where id = ?", String.class, petId))
                    .isEqualTo(AdoptionStatus.IN_PROCESS.name());
            assertThat(applicationStatuses()).containsExactlyInAnyOrder("APPROVED", "SUBMITTED");
            assertThat(historyCount(firstApplicationId) + historyCount(secondApplicationId)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from notifications where type = 'APPLICATION_STATUS_CHANGED' "
                    + "and reference_type = 'APPLICATION'", Integer.class)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private ApprovalAttempt approveConcurrently(UUID applicationId, CyclicBarrier barrier,
            AtomicInteger transactionsEntered) {
        try {
            transactions.execute(status -> {
                transactionsEntered.incrementAndGet();
                await(barrier);
                applications.updateStatus(shelterPrincipal, applicationId, ApplicationStatus.APPROVED,
                        "Aprobación concurrente");
                return null;
            });
            return new ApprovalAttempt(true, null);
        } catch (Throwable failure) {
            return new ApprovalAttempt(false, failure);
        }
    }

    private void insertUser(UUID id, String name, UserRole role) {
        jdbc.update("insert into users(id, full_name, email, password_hash, role) values (?, ?, ?, ?, ?)",
                id, name, id + "@example.test", "test-hash", role.name());
    }

    private void insertApplication(UUID id, UUID adopterId) {
        jdbc.update("insert into adoption_applications(id, pet_id, adopter_id, shelter_id, status, motivation) "
                        + "values (?, ?, ?, ?, 'SUBMITTED', 'Solicitud de prueba')",
                id, petId, adopterId, shelterId);
    }

    private String[] applicationStatuses() {
        return jdbc.query("select status from adoption_applications where pet_id = ? order by id", (result, row) ->
                result.getString("status"), petId).toArray(String[]::new);
    }

    private int historyCount(UUID applicationId) {
        return jdbc.queryForObject("select count(*) from application_status_history where application_id = ?",
                Integer.class, applicationId);
    }

    private static void setId(User user, UUID id) {
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", id);
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await(30, TimeUnit.SECONDS);
        } catch (Exception failure) {
            throw new IllegalStateException("Las dos transacciones no llegaron a la barrera", failure);
        }
    }

    private record ApprovalAttempt(boolean succeeded, Throwable failure) {}
}
