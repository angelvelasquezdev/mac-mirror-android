package com.angelsoft.macmirror.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "MacMirrorMasterKey"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_SIZE = 128
    }

    init {
        initMasterKey()
    }

    // 1. Initialize Keystore-backed Master Key for wrapping/unwrapping session keys
    private fun initMasterKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val spec = KeyGenParameterSpec.Builder(
                    MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            println("AndroidKeyStore provider not available: ${e.message}. This is normal in JVM tests.")
        }
    }

    private fun getMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val entry = keyStore.getEntry(MASTER_KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    // 2. Generate Ephemeral EC KeyPair for ECDH
    fun generateEphemeralKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        val ecSpec = ECGenParameterSpec("secp256r1")
        keyPairGenerator.initialize(ecSpec)
        return keyPairGenerator.generateKeyPair()
    }

    // 3. Compute shared secret via ECDH
    fun computeSharedSecret(myPrivateKey: PrivateKey, remotePublicKeyBytes: ByteArray): ByteArray {
        val keyFactory = KeyFactory.getInstance("EC")
        val keySpec = X509EncodedKeySpec(remotePublicKeyBytes)
        val remotePublicKey = keyFactory.generatePublic(keySpec)

        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(myPrivateKey)
        keyAgreement.doPhase(remotePublicKey, true)
        return keyAgreement.generateSecret()
    }

    // 4. Derive the Symmetric Key using SHA-256 KDF
    fun deriveSymmetricKey(sharedSecret: ByteArray, pin: String): SecretKey {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(sharedSecret)
        md.update(pin.toByteArray(Charsets.UTF_8))
        val derivedKeyBytes = md.digest()
        return SecretKeySpec(derivedKeyBytes, "AES")
    }

    // 5. Encrypt Derived Symmetric Key with Keystore Master Key to persist in DataStore
    fun encryptSessionKey(sessionKey: SecretKey): String {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getMasterKey())
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(sessionKey.encoded)

        val ivStr = Base64.encodeToString(iv, Base64.NO_WRAP)
        val encryptedStr = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        return "$ivStr:$encryptedStr"
    }

    // 6. Decrypt Session Key from DataStore
    fun decryptSessionKey(encryptedKeyBase64: String): SecretKey {
        val parts = encryptedKeyBase64.split(":")
        require(parts.size == 2) { "Invalid encrypted session key format" }
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, getMasterKey(), spec)

        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return SecretKeySpec(decryptedBytes, "AES")
    }

    // 7. Encrypt Notification Payload using the decrypted Session Key
    fun encryptPayload(plaintext: String, sessionKey: SecretKey): EncryptedResult {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, sessionKey)
        val iv = cipher.iv
        val ciphertextWithTag = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Split ciphertext and tag
        val ciphertextLength = ciphertextWithTag.size - (TAG_SIZE / 8)
        val ciphertext = ciphertextWithTag.copyOfRange(0, ciphertextLength)
        val tag = ciphertextWithTag.copyOfRange(ciphertextLength, ciphertextWithTag.size)

        return EncryptedResult(
            iv = Base64.encodeToString(iv, Base64.NO_WRAP),
            ciphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            tag = Base64.encodeToString(tag, Base64.NO_WRAP)
        )
    }

    data class EncryptedResult(
        val iv: String,
        val ciphertext: String,
        val tag: String
    )
}
