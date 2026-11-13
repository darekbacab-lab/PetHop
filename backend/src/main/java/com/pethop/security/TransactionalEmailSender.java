package com.pethop.security;

/** Envío de correos transaccionales (verificación de correo y recuperación de contraseña). */
public interface TransactionalEmailSender {
    boolean isConfigured();

    void send(String to, String subject, String text);
}
