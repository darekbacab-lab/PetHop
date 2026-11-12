package com.pethop.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@Converter
public class SensitiveDataConverter implements AttributeConverter<String,String> {
    private static final String PREFIX="enc:"; private static final int IV_SIZE=12; private static final SecureRandom RANDOM=new SecureRandom();
    private static byte[] key(){String raw=System.getenv("PETHOP_DATA_ENCRYPTION_KEY");if(raw==null||raw.isBlank())return null;try{byte[] value=Base64.getDecoder().decode(raw);if(value.length!=16&&value.length!=24&&value.length!=32)throw new IllegalStateException("PETHOP_DATA_ENCRYPTION_KEY debe ser Base64 de 16, 24 o 32 bytes.");return value;}catch(IllegalArgumentException e){throw new IllegalStateException("PETHOP_DATA_ENCRYPTION_KEY no es Base64 válido.",e);}}
    public String convertToDatabaseColumn(String value){if(value==null)return null;byte[] key=key();if(key==null||value.isBlank())return value;try{byte[] iv=new byte[IV_SIZE];RANDOM.nextBytes(iv);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv));byte[] encrypted=cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));byte[] all=new byte[iv.length+encrypted.length];System.arraycopy(iv,0,all,0,iv.length);System.arraycopy(encrypted,0,all,iv.length,encrypted.length);return PREFIX+Base64.getEncoder().encodeToString(all);}catch(Exception e){throw new IllegalStateException("No se pudo proteger un dato sensible.",e);}}
    public String convertToEntityAttribute(String value){if(value==null||!value.startsWith(PREFIX))return value;byte[] key=key();if(key==null)throw new IllegalStateException("PETHOP_DATA_ENCRYPTION_KEY es obligatorio para leer datos protegidos.");try{byte[] all=Base64.getDecoder().decode(value.substring(PREFIX.length()));if(all.length<=IV_SIZE)throw new IllegalStateException("Dato protegido inválido.");Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,all,0,IV_SIZE));return new String(cipher.doFinal(all,IV_SIZE,all.length-IV_SIZE),StandardCharsets.UTF_8);}catch(Exception e){throw new IllegalStateException("No se pudo descifrar un dato sensible.",e);}}
}
