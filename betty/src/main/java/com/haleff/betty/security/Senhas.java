package com.haleff.betty.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Guarda senhas com hash PBKDF2 + sal (nunca a senha em texto puro). */
public final class Senhas {

    private static final int ITERACOES = 120_000;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Senhas() {
    }

    public static String hash(String senha) {
        byte[] sal = new byte[16];
        ALEATORIO.nextBytes(sal);
        return Base64.getEncoder().encodeToString(sal) + ":" + Base64.getEncoder().encodeToString(pbkdf2(senha, sal));
    }

    public static boolean confere(String senha, String guardado) {
        String[] partes = guardado.split(":");
        if (partes.length != 2) return false;
        byte[] sal = Base64.getDecoder().decode(partes[0]);
        byte[] esperado = Base64.getDecoder().decode(partes[1]);
        return MessageDigest.isEqual(esperado, pbkdf2(senha, sal));
    }

    private static byte[] pbkdf2(String senha, byte[] sal) {
        try {
            PBEKeySpec spec = new PBEKeySpec(senha.toCharArray(), sal, ITERACOES, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível calcular o hash da senha.", e);
        }
    }
}
