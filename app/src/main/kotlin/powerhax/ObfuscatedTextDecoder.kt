package astral.loader

object ObfuscatedTextDecoder {

    private const val INITIAL_STATE = 8_186_484_168_865_098L
    private const val STATE_BASE = 274_877_906_944L
    private const val STATE_MULTIPLIER = 16_384 + 4_887

    fun decode(encoded: String): String {
        val decoded = StringBuilder(encoded.length / 2)
        var state = INITIAL_STATE

        for (index in encoded.indices step 2) {
            val hexByte = encoded.substring(index, minOf(index + 2, encoded.length))
            val stateRemainder = state % STATE_BASE
            val stateQuotient = (state - stateRemainder) / STATE_BASE
            val rotation = (stateQuotient % 128).toInt()
            val encryptedByte = hexByte.toInt(16)
            val multiplier = (stateQuotient - rotation) / 128
            val plainByte = ((encryptedByte + multiplier) * (2 * rotation + 1) % 256).toInt()

            state = stateRemainder * STATE_MULTIPLIER + stateQuotient + encryptedByte + plainByte
            decoded.append(plainByte.toChar())
        }

        return decoded.toString()
    }
}
