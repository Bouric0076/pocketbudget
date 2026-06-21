package com.ics2300.pocketbudget.utils

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtils {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KEY_LENGTH_BITS = 256
    private const val ITERATIONS = 10000
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    fun encrypt(data: ByteArray, password: CharArray): ByteArray {
        val random = SecureRandom()
        
        // 1. Generate random salt
        val salt = ByteArray(SALT_LENGTH_BYTES)
        random.nextBytes(salt)
        
        // 2. Derive key using PBKDF2
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(DERIVATION_ALGORITHM)
        val derivedKeyBytes = factory.generateSecret(spec).encoded
        val secretKey = SecretKeySpec(derivedKeyBytes, "AES")
        
        // 3. Generate random IV
        val iv = ByteArray(IV_LENGTH_BYTES)
        random.nextBytes(iv)
        
        // 4. Encrypt with AES-GCM
        val cipher = Cipher.getInstance(ALGORITHM)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
        val ciphertext = cipher.doFinal(data)
        
        // 5. Combine salt, IV, and ciphertext
        val combined = ByteArray(SALT_LENGTH_BYTES + IV_LENGTH_BYTES + ciphertext.size)
        System.arraycopy(salt, 0, combined, 0, SALT_LENGTH_BYTES)
        System.arraycopy(iv, 0, combined, SALT_LENGTH_BYTES, IV_LENGTH_BYTES)
        System.arraycopy(ciphertext, 0, combined, SALT_LENGTH_BYTES + IV_LENGTH_BYTES, ciphertext.size)
        
        return combined
    }

    fun decrypt(combined: ByteArray, password: CharArray): ByteArray {
        if (combined.size < SALT_LENGTH_BYTES + IV_LENGTH_BYTES) {
            throw IllegalArgumentException("Data is too short to be an encrypted file.")
        }
        
        // 1. Extract salt
        val salt = ByteArray(SALT_LENGTH_BYTES)
        System.arraycopy(combined, 0, salt, 0, SALT_LENGTH_BYTES)
        
        // 2. Extract IV
        val iv = ByteArray(IV_LENGTH_BYTES)
        System.arraycopy(combined, SALT_LENGTH_BYTES, iv, 0, IV_LENGTH_BYTES)
        
        // 3. Derive key
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(DERIVATION_ALGORITHM)
        val derivedKeyBytes = factory.generateSecret(spec).encoded
        val secretKey = SecretKeySpec(derivedKeyBytes, "AES")
        
        // 4. Decrypt
        val ciphertextLength = combined.size - SALT_LENGTH_BYTES - IV_LENGTH_BYTES
        val ciphertext = ByteArray(ciphertextLength)
        System.arraycopy(combined, SALT_LENGTH_BYTES + IV_LENGTH_BYTES, ciphertext, 0, ciphertextLength)
        
        val cipher = Cipher.getInstance(ALGORITHM)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
        
        return cipher.doFinal(ciphertext)
    }
}
