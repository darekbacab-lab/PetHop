package com.pethop.adoption;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ApplicationAnswerRepository extends JpaRepository<ApplicationAnswer,UUID> { }
