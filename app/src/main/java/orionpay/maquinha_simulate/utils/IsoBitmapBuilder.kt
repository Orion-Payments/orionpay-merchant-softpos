package orionpay.maquinha_simulate.utils

import java.util.BitSet

/**
 * Utilitário para construir o Bitmap ISO 8583 (Primary e Secondary).
 */
class IsoBitmapBuilder {
    private val bits = BitSet(128)

    /**
     * Define um campo como presente no bitmap.
     * @param field Número do campo (1 a 128)
     */
    fun setField(field: Int): IsoBitmapBuilder {
        if (field in 1..128) {
            bits.set(field - 1)
        }
        return this
    }

    /**
     * Constrói a representação Hexadecimal do Bitmap.
     */
    fun buildHex(): String {
        // Se houver bits acima de 64, o Bit 1 (Secondary Bitmap) deve estar ligado
        val hasSecondary = (65..128).any { bits.get(it - 1) }
        if (hasSecondary) bits.set(0)

        val size = if (hasSecondary) 128 else 64
        val bytes = ByteArray(size / 8)
        
        for (i in 0 until size) {
            if (bits.get(i)) {
                // ISO 8583 usa Big-Endian para os bits dentro do byte (mais significativo primeiro)
                bytes[i / 8] = (bytes[i / 8].toInt() or (1 shl (7 - (i % 8)))).toByte()
            }
        }
        return bytesToHex(bytes)
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
