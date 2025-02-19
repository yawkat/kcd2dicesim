package at.yawk.kcd2dicesim

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

const val JOKER = 6.toByte()

/**
 * Set of thrown dice values (up to six dice, 1-6 eyes each). Internal encoding is three bits per die.
 */
@JvmInline
@Serializable
value class DiceThrow private constructor(val value: Int) {
    /**
     * Create a new throw. Inputs must be in `0..6`
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
                if (die < 0 || die > JOKER) {
                    return false
                }
            }
            return true
        }

    private fun valid(jokerMask: Byte): Boolean {
        for (i in 0 until length) {
            val j = (jokerMask.toInt() ushr i) and 1
            val die = get(i)
            if (die < j || die > 5 + j) {
                return false
            }
        }
        return true
    }

    /**
     * Get the die value at the given index (`0..6`).
     */
    operator fun get(i: Int): Byte = (((value ushr (i * BITS_PER_DIE)) and ((1 shl BITS_PER_DIE) - 1)) - 1).toByte()

    /**
     * Sort the dice values.
     */
    fun sorted(): DiceThrow = DiceThrow(Caches.SORTED_CACHE[value])

    fun toArray() = ByteArray(length) { get(it) }

    override fun toString() = "DiceThrow" + (toArray().joinToString(",", "[", "]"))

    private object Caches {
        const val CACHE_SIZE = 1 shl (BITS_PER_DIE * 6)

        val SORTED_CACHE = IntArray(CACHE_SIZE)

        val MASK = Array(CACHE_SIZE) {
            val thr = DiceThrow(it)
            if (thr.valid) {
                IntArray(1 shl thr.length) { mask ->
                    thr.mask0(mask.toByte()).value
                }
            } else {
                null
            }
        }

        val SINGLE_SCORE = ByteArray(CACHE_SIZE)

        val MULTI_SCORE = ByteArray(CACHE_SIZE)

        private fun addSingleScore(thr: DiceThrow, score: Score) {
            SINGLE_SCORE[thr.value] = score.toCompactByte()

            for (mask in 1..((1 shl thr.length) - 2)) {
                val masked = thr.mask(mask.toByte())
                val jokersToAdd = thr.length - masked.length
                val jokersMask = ((1 shl (jokersToAdd * BITS_PER_DIE)) - 1) shl (masked.length * BITS_PER_DIE)
                val combined = DiceThrow(jokersMask or masked.value)
                SINGLE_SCORE[combined.value] = score.toCompactByte()
            }
        }

        init {
            addSingleScore(DiceThrow(0.toByte()), Score(100))
            addSingleScore(DiceThrow(4.toByte()), Score(50))

            addSingleScore(DiceThrow(0, 1, 2, 3, 4), Score(500))
            addSingleScore(DiceThrow(1, 2, 3, 4, 5), Score(750))
            addSingleScore(DiceThrow(0, 1, 2, 3, 4, 5), Score(1500))
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
                addSingleScore(DiceThrow(i, i, i), baseScore)
                addSingleScore(DiceThrow(i, i, i, i), (baseScore * 2))
                addSingleScore(DiceThrow(i, i, i, i, i), (baseScore * 4))
                addSingleScore(DiceThrow(i, i, i, i, i, i), (baseScore * 8))
            }
            for (i in 0 until CACHE_SIZE) {
                DiceThrow(i).initCaches()
            }
        }

        init {
            println("DiceThrow initialized")
        }
    }

    companion object {
        private const val BITS_PER_DIE = 3

        fun maskForNumberOfDice(numberOfDice: Int): Int {
            return (1 shl (numberOfDice * BITS_PER_DIE)) - 1
        }

        // internal to access private constructor
        internal inline fun forEachThrow(numberOfDice: Int, jokerMask: Byte = 0, f: (DiceThrow) -> Unit) {
            val lo = maskForNumberOfDice(numberOfDice - 1) + 1 // this works by accident for numberOfDice == 0
            val hi = maskForNumberOfDice(numberOfDice)
            for (i in lo..hi) {
                val thr = DiceThrow(i)
                if (thr.valid(jokerMask)) {
                    f(thr)
                }
            }
        }

        fun combine(arr: ByteArray): Int {
            var i = 0
            for (byte in arr.reversed()) {
                i = (i shl BITS_PER_DIE) + (byte + 1)
            }
            return i
        }
    }

    private fun initCaches() {
        if (valid) {
            val sorted = sort0()
            Caches.SORTED_CACHE[value] = sorted.value
            Caches.SINGLE_SCORE[value] = Caches.SINGLE_SCORE[sorted.value]
            Caches.MULTI_SCORE[value] = multiScore0().toCompactByte()
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
                masked = (masked and clipMask) or ((masked ushr BITS_PER_DIE) and clipMask.inv())
            }
        }
        return DiceThrow(masked)
    }

    /**
     * Return a new throw that only keeps those dice values where the corresponding bit in [mask] is set.
     */
    fun mask(mask: Byte): DiceThrow = DiceThrow(Caches.MASK[value]!![mask.toInt()])

    /**
     * Get the score for this throw as a *single* combination. For example, if this throw is `12345`, it will return a
     * score of 500, but if this is 112345 it will return 0.
     */
    fun selectionScoreSingle(): Score = Score.fromCompactByte(Caches.SINGLE_SCORE[value])

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
    fun multiScore(): Score = Score.fromCompactByte(Caches.MULTI_SCORE[value])
}