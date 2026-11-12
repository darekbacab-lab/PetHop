package com.pethop.security;
import org.springframework.context.annotation.Profile; import org.springframework.stereotype.Component;
@Component @Profile("prod") public class DataEncryptionKeyCheck { public DataEncryptionKeyCheck(){String key=System.getenv("PETHOP_DATA_ENCRYPTION_KEY");if(key==null||key.isBlank())throw new IllegalStateException("PETHOP_DATA_ENCRYPTION_KEY es obligatorio en producción.");} }
