package orionpay.maquinha_simulate.utils

/**
 * Utilitário para geração de PIN Block (ISO 9564-1).
 */
object PinBlockUtil {

    /**
     * Gera o PIN Block Formato 0 (ANSI X9.8).
     * @param pin Senha do cliente (ex: "1234")
     * @param pan Número do cartão completo (ex: "4567890123456789")
     * @return PIN Block em Hexadecimal (16 caracteres)
     */
    fun format0(pin: String, pan: String): String {
        try {
            if (pin.length < 4 || pin.length > 12) return ""
            
            // 1. PIN Block: 0 + L (hex nibble) + PIN + F padding
            val pinLenHex = Integer.toHexString(pin.length).uppercase()
            val pinPart = "0$pinLenHex$pin"
            val pinBlockHex = pinPart.padEnd(16, 'F')
            
            // 2. PAN Block: 0000 + 12 dígitos mais à direita (excluindo o check digit)
            val cleanPan = pan.filter { it.isDigit() }
            if (cleanPan.length < 13) return ""
            
            // Pega os 12 dígitos antes do último
            val panPart = cleanPan.substring(cleanPan.length - 13, cleanPan.length - 1)
            val panBlockHex = "0000$panPart"
            
            // 3. XOR entre os dois blocos
            return xorHex(pinBlockHex, panBlockHex)
        } catch (e: Exception) {
            return ""
        }
    }

    private fun xorHex(hex1: String, hex2: String): String {
        val b1 = hexToBytes(hex1)
        val b2 = hexToBytes(hex2)
        val res = ByteArray(8)
        for (i in 0..7) {
            res[i] = (b1[i].toInt() xor b2[i].toInt()).toByte()
        }
        return bytesToHex(res)
    }

    private fun hexToBytes(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
        }
        return data
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF".toCharArray()
        val hex = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hex[i * 2] = hexChars[v ushr 4]
            hex[i * 2 + 1] = hexChars[v and 0x0F]
        }
        return String(hex)
    }
}
