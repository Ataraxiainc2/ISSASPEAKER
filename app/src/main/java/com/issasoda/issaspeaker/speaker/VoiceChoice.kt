package com.issasoda.issaspeaker.speaker

/** The voice announcements use: the phone's (free, private) or an AI provider's (the owner's key and credit). */
sealed interface VoiceChoice {
    /** The phone engine's voice for the app language. */
    data object Automatic : VoiceChoice

    data class Phone(val name: String) : VoiceChoice

    data class Ai(val providerId: String, val model: String, val voice: String) : VoiceChoice

    /** Stored form; null for [Automatic]. */
    fun encode(): String? = when (this) {
        Automatic -> null
        is Phone -> "phone:$name"
        is Ai -> "ai:$providerId|$model|$voice"
    }

    companion object {
        fun decode(stored: String?): VoiceChoice {
            if (stored == null) return Automatic
            val kind = stored.substringBefore(':')
            val value = stored.substringAfter(':', "")
            val parts = value.split('|')
            return when {
                kind == "phone" && value.isNotEmpty() -> Phone(value)
                kind == "ai" && parts.size == AI_PARTS && parts.none { it.isEmpty() } -> Ai(parts[0], parts[1], parts[2])
                // A bare engine voice name, stored before AI voices existed.
                ':' !in stored -> Phone(stored)
                else -> Automatic
            }
        }

        private const val AI_PARTS = 3
    }
}
