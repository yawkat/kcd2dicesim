package at.yawk.kcd2dicesim

import kotlin.jvm.JvmInline

/**
 * Optimized score implementation. Since scores are always a multiple of 50, this class removes that factor.
 */
@JvmInline
value class Score private constructor(private val value: Byte) : Comparable<Score> {
    constructor(value: Int) : this((value / SCORE_DIVIDER).toByte())

    operator fun plus(other: Score): Score = Score((value + other.value).toByte())

    operator fun minus(other: Score): Score = Score((value - other.value).toByte())

    operator fun times(scale: Int): Score = Score((value * scale).toByte())

    override operator fun compareTo(other: Score): Int = (value - other.value)

    fun toInt() = value * SCORE_DIVIDER

    fun toCompactByte() = value

    override fun toString() = "Score(${toInt()})"

    companion object {
        const val SCORE_DIVIDER = 50

        fun fromCompactByte(value: Byte) = Score(value)
    }
}