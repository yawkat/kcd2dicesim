package at.yawk.kcd2dicesim

import kotlin.jvm.JvmInline

/**
 * Set of thrown dice values (up to six dice, 1-6 eyes each). Internal encoding is three bits per die.
 */
@JvmInline
value class DiceThrow private constructor(val value: Int) {
    /**
     * Create a new throw. Inputs must be in `0..5`
     */
    constructor(vararg values: Byte) : this(combine(values))

    /**
     * Get the number of dice in this throw.
     */
    val length: Int
        get() {
            val highBit = value.takeHighestOneBit()
            return if (highBit == 0) {
                0
            } else {
                (highBit.countTrailingZeroBits() / 3) + 1
            }
        }

    private val valid: Boolean
        get() {
            for (i in 0 until length) {
                val die = get(i)
                if (die < 0 || die > 5) {
                    return false
                }
            }
            return true
        }

    /**
     * Get the die value at the given index (`0..5`).
     */
    operator fun get(i: Int): Byte = (((value ushr (i * 3)) and 7) - 1).toByte()

    /**
     * Sort the dice values.
     */
    fun sorted(): DiceThrow = DiceThrow(SORTED_CACHE[value])

    fun toArray() = ByteArray(length) { get(it) }

    override fun toString() = "DiceThrow" + (toArray().joinToString(",", "[", "]"))

    fun toCompactInt() = value

    companion object {
        private const val CACHE_SIZE = 1 shl (3 * 6)

        private val SORTED_CACHE = IntArray(CACHE_SIZE)

        private val MASK = Array(CACHE_SIZE) {
            val thr = DiceThrow(it)
            if (thr.valid) {
                IntArray(1 shl thr.length) { mask ->
                    thr.mask0(mask.toByte()).value
                }
            } else {
                null
            }
        }

        private val SINGLE_SCORE = ByteArray(CACHE_SIZE)

        private val MULTI_SCORE = ByteArray(CACHE_SIZE)

        init {
            SINGLE_SCORE[DiceThrow(0.toByte()).value] = Score(100).toCompactByte()
            SINGLE_SCORE[DiceThrow(4.toByte()).value] = Score(50).toCompactByte()
            SINGLE_SCORE[DiceThrow(0, 1, 2, 3, 4).value] = Score(500).toCompactByte()
            SINGLE_SCORE[DiceThrow(1, 2, 3, 4, 5).value] = Score(750).toCompactByte()
            SINGLE_SCORE[DiceThrow(0, 1, 2, 3, 4, 5).value] = Score(1500).toCompactByte()
            for (j in 0..5) {
                val i = j.toByte()
                val baseScore = when (j) {
                    0 -> Score(1000)
                    1 -> Score(200)
                    2 -> Score(300)
                    3 -> Score(400)
                    4 -> Score(500)
                    5 -> Score(600)
                    else -> throw AssertionError()
                }
                SINGLE_SCORE[DiceThrow(i, i, i).value] = baseScore.toCompactByte()
                SINGLE_SCORE[DiceThrow(i, i, i, i).value] = (baseScore * 2).toCompactByte()
                SINGLE_SCORE[DiceThrow(i, i, i, i, i).value] = (baseScore * 4).toCompactByte()
                SINGLE_SCORE[DiceThrow(i, i, i, i, i, i).value] = (baseScore * 8).toCompactByte()
            }
            for (i in 0 until CACHE_SIZE) {
                DiceThrow(i).initCaches()
            }
        }

        private fun combine(arr: ByteArray): Int {
            var i = 0
            for (byte in arr.reversed()) {
                i = (i shl 3) + (byte + 1)
            }
            return i
        }

        init {
            println("DiceThrow initialized")
        }

        private fun maskForNumberOfDice(numberOfDice: Int): Int {
            return (1 shl (numberOfDice * 3)) - 1
        }

        // internal to access private constructor
        internal inline fun forEachThrow(numberOfDice: Int, f: (DiceThrow) -> Unit) {
            val lo = maskForNumberOfDice(numberOfDice - 1) + 1 // this works by accident for numberOfDice == 0
            val hi = maskForNumberOfDice(numberOfDice)
            for (i in lo..hi) {
                val thr = DiceThrow(i)
                if (thr.valid) {
                    f(thr)
                }
            }
        }

        fun fromCompactInt(int: Int) = DiceThrow(int)
    }

    private fun initCaches() {
        if (valid) {
            val sorted = sort0()
            SORTED_CACHE[value] = sorted.value
            SINGLE_SCORE[value] = SINGLE_SCORE[sorted.value]
            MULTI_SCORE[value] = multiScore0().toCompactByte()
        }
    }

    private fun sort0(): DiceThrow {
        return DiceThrow(*toArray().sortedArray())
    }

    private fun mask0(mask: Byte): DiceThrow {
        var masked = value
        for (i in length - 1 downTo 0) {
            val selected = ((mask.toInt() ushr i) and 1) == 1
            if (!selected) {
                val clipMask = maskForNumberOfDice(i)
                masked = (masked and clipMask) or ((masked ushr 3) and clipMask.inv())
            }
        }
        return DiceThrow(masked)
    }

    /**
     * Return a new throw that only keeps those dice values where the corresponding bit in [mask] is set.
     */
    fun mask(mask: Byte): DiceThrow = DiceThrow(MASK[value]!![mask.toInt()])

    /**
     * Get the score for this throw as a *single* combination. For example, if this throw is `12345`, it will return a
     * score of 500, but if this is 112345 it will return 0.
     */
    fun selectionScoreSingle(): Score = Score.fromCompactByte(SINGLE_SCORE[value])

    private fun multiScore0(): Score {
        val direct = selectionScoreSingle()
        if (direct != Score(0)) {
            return direct
        }
        var best = Score(0)
        val peak = 1 shl length
        for (mask in 1 until peak) {
            val a = mask(mask.toByte())
            val b = mask((mask.inv() and (peak - 1)).toByte())
            val aScore = a.selectionScoreSingle()
            if (aScore != Score(0)) {
                val bScore = b.multiScore()
                if (bScore != Score(0)) {
                    val combined = aScore + bScore
                    if (combined > best) {
                        best = combined
                    }
                }
            }
        }
        return best
    }

    /**
     * Get the score for this throw, including multiple combinations. For example, if this throw is 112345, this method
     * returns 600 (500 for 12345 and 100 for the remaining 1). If this throw contains dice that cannot be combined,
     * this method returns 0.
     */
    fun multiScore(): Score = Score.fromCompactByte(MULTI_SCORE[value])
}