package com.interviewcoach.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * API Key 加解密组件。使用 AES/GCM/NoPadding 算法对用户自定义 Provider 的 API Key
 * 进行加密存储和解密使用，密钥通过 {@code app.ai.encryption-key} 配置注入。
 */
@Component
public class ApiKeyEncryption {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private final SecretKey secretKey;

    public ApiKeyEncryption(@Value("${app.ai.encryption-key}") String encryptionKey) {
        byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 加密明文 API Key。每次加密生成随机 IV，返回 Base64 编码的 IV + 密文。
     *
     * @param plaintext 明文 API Key
     * @return Base64 编码的加密字符串
     * @throws IllegalStateException 加密失败时
     */
    public String encrypt(String plaintext) {
        try {
            // 1. 生成随机 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // 2. 使用 AES-GCM 加密
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

            // 3. 拼接 IV + 密文并 Base64 编码
            byte[] result = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt API key", ex);
        }
    }

    /**
     * 解密 Base64 编码的加密字符串，返回明文 API Key。
     *
     * @param encrypted Base64 编码的加密字符串
     * @return 明文 API Key
     * @throws IllegalStateException 解密失败时
     */
    public String decrypt(String encrypted) {
        try {
            // 1. Base64 解码并提取 IV
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, iv.length);

            // 2. 使用 AES-GCM 解密密文部分
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plaintext = cipher.doFinal(decoded, iv.length, decoded.length - iv.length);
            return new String(plaintext, "UTF-8");
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt API key", ex);
        }
    }
}
