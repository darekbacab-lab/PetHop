package com.pethop.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Regresión del CI #21: BrevoEmailSender tiene dos constructores (uno para Spring y otro para
 * las pruebas). Sin @Autowired en el principal, Spring busca un constructor sin argumentos,
 * no lo encuentra y el contexto no arranca (Failed to load ApplicationContext).
 */
class BrevoEmailSenderBeanTest {
    @Test
    void springInstantiatesSenderAsBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(BrevoProperties.class, BrevoEmailSender.class);
            context.refresh();
            assertThat(context.getBean(BrevoEmailSender.class)).isNotNull();
            assertThat(context.getBean(BrevoEmailSender.class).isConfigured()).isFalse();
        }
    }
}
