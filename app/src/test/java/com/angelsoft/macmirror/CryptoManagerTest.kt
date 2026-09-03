package com.angelsoft.macmirror

import android.content.Context
import android.util.Base64
import com.angelsoft.macmirror.security.CryptoManager
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import javax.crypto.spec.SecretKeySpec

class CryptoManagerTest {

    private val context: Context = mockk(relaxed = true)
    private lateinit var cryptoManager: CryptoManager
    private val mockMasterKey = SecretKeySpec(ByteArray(32) { 1.toByte() }, "AES")

    @Before
    fun setUp() {
        // Mock Base64 which is an Android framework dependency (not available in raw JVM unit tests)
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg<ByteArray>())
        }
        every { Base64.decode(any<String>(), any()) } answers {
            java.util.Base64.getDecoder().decode(firstArg<String>())
        }

        // Create CryptoManager and mock the KeyStore-backed getMasterKey() method
        cryptoManager = spyk(CryptoManager(context), recordPrivateCalls = true)
        every { cryptoManager["getMasterKey"]() } returns mockMasterKey
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testEphemeralKeyPairGeneration() {
        val keyPair = cryptoManager.generateEphemeralKeyPair()
        assertNotNull(keyPair)
        assertEquals("EC", keyPair.public.algorithm)
        assertEquals("EC", keyPair.private.algorithm)
    }

    @Test
    fun testECDHKeyAgreementAndDerivation() {
        // Generate two key pairs (Client & Server)
        val clientKeyPair = cryptoManager.generateEphemeralKeyPair()
        val serverKeyPair = cryptoManager.generateEphemeralKeyPair()

        // Get public keys in encoded format
        val clientPubBytes = clientKeyPair.public.encoded
        val serverPubBytes = serverKeyPair.public.encoded

        // Compute shared secret from both sides
        val clientSecret = cryptoManager.computeSharedSecret(clientKeyPair.private, serverPubBytes)
        val serverSecret = cryptoManager.computeSharedSecret(serverKeyPair.private, clientPubBytes)

        // Verify shared secrets match
        assertArrayEquals(clientSecret, serverSecret)

        // Derive key from shared secret using a PIN
        val pin = "123456"
        val clientDerivedKey = cryptoManager.deriveSymmetricKey(clientSecret, pin)
        val serverDerivedKey = cryptoManager.deriveSymmetricKey(serverSecret, pin)

        // Verify derived keys match
        assertArrayEquals(clientDerivedKey.encoded, serverDerivedKey.encoded)
    }

    @Test
    fun testPayloadEncryptionDecryption() {
        // Derive a key
        val derivedKey = SecretKeySpec(ByteArray(32) { 9.toByte() }, "AES")
        val plaintext = "Hello macOS, this is a secure notification payload!"

        // Encrypt payload
        val encryptedResult = cryptoManager.encryptPayload(plaintext, derivedKey)
        assertNotNull(encryptedResult.iv)
        assertNotNull(encryptedResult.ciphertext)
        assertNotNull(encryptedResult.tag)

        // Decrypt using JDK native cipher to verify AES-GCM output is valid
        val iv = java.util.Base64.getDecoder().decode(encryptedResult.iv)
        val ciphertext = java.util.Base64.getDecoder().decode(encryptedResult.ciphertext)
        val tag = java.util.Base64.getDecoder().decode(encryptedResult.tag)

        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        val spec = javax.crypto.spec.GCMParameterSpec(128, iv)
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, derivedKey, spec)

        // Combine ciphertext and tag for JCE's doFinal in GCM mode
        val ciphertextWithTag = ciphertext + tag
        val decryptedBytes = cipher.doFinal(ciphertextWithTag)
        val decryptedText = String(decryptedBytes, Charsets.UTF_8)

        assertEquals(plaintext, decryptedText)
    }

    @Test
    fun testSessionKeyWrapping() {
        val sessionKey = SecretKeySpec(ByteArray(32) { 5.toByte() }, "AES")

        // Wrap key
        val encryptedKeyStr = cryptoManager.encryptSessionKey(sessionKey)
        assertTrue(encryptedKeyStr.contains(":"))

        // Unwrap key
        val decryptedKey = cryptoManager.decryptSessionKey(encryptedKeyStr)
        assertArrayEquals(sessionKey.encoded, decryptedKey.encoded)
    }
}
