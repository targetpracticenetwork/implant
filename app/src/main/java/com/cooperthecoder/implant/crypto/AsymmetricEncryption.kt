package com.cooperthecoder.implant.crypto

interface AsymmetricEncryption {
    open class CryptoException(e: String) : Exception(e)
    class DecryptionException(e: String) : CryptoException(e)
    class EncryptionException(e: String) : CryptoException(e)

    fun encrypt(plaintext: ByteArray): ByteArray
    fun decrypt(nonceAndCiphertext: ByteArray): ByteArray
    fun encrypt(plaintext: String): String
    fun decrypt(nonceAndCiphertext: String): String
}