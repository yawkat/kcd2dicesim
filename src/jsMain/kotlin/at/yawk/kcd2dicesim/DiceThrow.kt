package at.yawk.kcd2dicesim

val COMBINATION_COUNTS = intArrayOf(
    1,
    6,
    6 * 6,
    6 * 6 * 6,
    6 * 6 * 6 * 6,
    6 * 6 * 6 * 6 * 6,
    6 * 6 * 6 * 6 * 6 * 6,
)

value class DiceThrow private constructor(val value: Int) {
    constructor(i: Int, numberOfDice: Int) : this(i + numberOfDice * LENGTH_OFFSET) {
        require(numberOfDice >= 0)
        require(numberOfDice <= 6)
    }

    constructor(vararg values: Byte) : this(combine(values), values.size)

    val length: Int
        get() = value / LENGTH_OFFSET

    operator fun get(i: Int): Byte = ((value / COMBINATION_COUNTS[i]) % 6).toByte()

    fun sorted() = SORTED_CACHE[value]

    fun toArray() = ByteArray(length) { get(it) }

    override fun toString() = "DiceThrow" + (toArray().joinToString(",", "[", "]"))

    companion object {
        private val LENGTH_OFFSET = COMBINATION_COUNTS.last()

        private val ALL_THROW_COUNT = 7 * LENGTH_OFFSET

        private val SORTED_CACHE = Array(ALL_THROW_COUNT) { i ->
            DiceThrow(i).sort0()
        }

        private val MASK = Array(ALL_THROW_COUNT) {
            val thr = DiceThrow(it)
            Array(1 shl thr.length) { mask ->
                thr.mask0(mask.toByte())
            }
        }

        private val SINGLE_SCORE = Array(ALL_THROW_COUNT) { Score(0) }

        init {
            SINGLE_SCORE[DiceThrow(0.toByte()).value] = Score(100)
            SINGLE_SCORE[DiceThrow(4.toByte()).value] = Score(50)
            SINGLE_SCORE[DiceThrow(0, 1, 2, 3, 4).value] = Score(500)
            SINGLE_SCORE[DiceThrow(1, 2, 3, 4, 5).value] = Score(750)
            SINGLE_SCORE[DiceThrow(0, 1, 2, 3, 4, 5).value] = Score(1500)
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
                SINGLE_SCORE[DiceThrow(i, i, i).value] = baseScore
                SINGLE_SCORE[DiceThrow(i, i, i, i).value] = baseScore * 2
                SINGLE_SCORE[DiceThrow(i, i, i, i, i).value] = baseScore * 4
                SINGLE_SCORE[DiceThrow(i, i, i, i, i, i).value] = baseScore * 8
            }
            for (i in 0 until ALL_THROW_COUNT) {
                val thr = DiceThrow(i)
                SINGLE_SCORE[thr.value] = SINGLE_SCORE[thr.sorted().value]
            }
        }

        private val MULTI_SCORE = Array(ALL_THROW_COUNT) { Score(0) }

        init {
            for (i in 0 until ALL_THROW_COUNT) {
                MULTI_SCORE[i] = DiceThrow(i).multiScore0()
            }
        }

        private fun combine(arr: ByteArray): Int {
            var i = 0
            for (byte in arr.reversed()) {
                i = i * 6 + byte
            }
            return i
        }

        init {
            println("DiceThrow initialized")
        }
    }

    private fun sort0(): DiceThrow {
        return DiceThrow(*toArray().sortedArray())
    }

    private fun mask0(mask: Byte): DiceThrow {
        var masked = value % LENGTH_OFFSET
        for (i in length - 1 downTo 0) {
            val selected = ((mask.toInt() ushr i) and 1) == 1
            if (!selected) {
                masked = masked % COMBINATION_COUNTS[i] + (masked / COMBINATION_COUNTS[i + 1]) * COMBINATION_COUNTS[i]
            }
        }
        return DiceThrow(masked, mask.countOneBits())
    }

    fun mask(mask: Byte) = MASK[value][mask.toInt()]

    fun selectionScoreSingle() = SINGLE_SCORE[value]

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
            val bScore = b.multiScore()
            if (aScore != Score(0) && bScore != Score(0)) {
                val combined = aScore + bScore
                if (combined > best) {
                    best = combined
                }
            }
        }
        return best
    }

    fun multiScore() = MULTI_SCORE[value]
}