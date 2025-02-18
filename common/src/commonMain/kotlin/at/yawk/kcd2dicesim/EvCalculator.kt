package at.yawk.kcd2dicesim

import kotlin.jvm.JvmInline
import kotlin.math.roundToInt

class EvCalculator(private val limit: Score, private val allDice: DieBag) {
    private var cacheMask = 511

    private var cacheKeys = LongArray(cacheMask + 1)
    private var cacheValues = Ev.Arr(cacheMask + 1)
    private var cacheSize = 0

    private fun probe(key: Long): Int {
        var i = mix(key).toInt() and cacheMask
        while (cacheKeys[i] != 0L && cacheKeys[i] != key) {
            i = (i + 1) and cacheMask
        }
        return i
    }

    private fun growCache() {
        val capacity = cacheMask + 1
        val oldKeys = cacheKeys
        val oldValues = cacheValues
        val newCapacity = capacity * 2
        cacheMask = newCapacity - 1
        cacheKeys = LongArray(newCapacity)
        cacheValues = Ev.Arr(newCapacity)

        for (i in oldKeys.indices) {
            val key = oldKeys[i]
            if (key != 0L) {
                val pos = probe(key)
                cacheKeys[pos] = key
                cacheValues[pos] = oldValues[i]
            }
        }
    }

    fun calculateEv(oldScore: Score, remainingDice: DieBag): Ev {
        return calculateEvInl(oldScore, remainingDice)
    }

    @Suppress("NOTHING_TO_INLINE") // reduce recursion stack depth
    private inline fun calculateEvInl(oldScore: Score, remainingDice: DieBag): Ev {
        val key = (oldScore.toCompactByte().toLong() and 0xffL) or (remainingDice.toCompactLong() shl 8)
        var i = probe(key)
        if (cacheKeys[i] == 0L) {
            if (cacheSize > (cacheMask ushr 1)) {
                growCache()
            }

            val result = calculateEv0(oldScore, remainingDice)
            i = probe(key)
            if (cacheKeys[i] == 0L) {
                cacheSize++
                cacheKeys[i] = key
                cacheValues[i] = result
            }
            return result
        } else {
            return cacheValues[i]
        }
    }

    private fun mix(l: Long): Long {
        // stolen from fastutil and they stole it from Koloboke
        var h = l * -7046029254386353131L
        h = h xor (h ushr 32)
        return h xor (h ushr 16)
    }

    private fun calculateEv0(oldScore: Score, remainingDice: DieBag): Ev {
        val n = remainingDice.size
        if (n == 0) {
            return calculateEv(oldScore, allDice)
        }
        var totalEv = Ev.WeightedSum.zero()
        DiceThrow.forEachThrow(n) { thr ->
            var weight = 1
            for (i in 0 until n) {
                weight *= remainingDice[i].getWeight(thr[i])
            }
            if (weight != 0) {
                totalEv = totalEv.plus(bestEv(oldScore, thr, remainingDice, null), weight)
            }
        }
        var totalWeight = 1
        for (i in 0 until n) {
            totalWeight *= remainingDice[i].totalWeight
        }
        return totalEv.div(totalWeight)
    }

    fun bestEv(oldScore: Score, thr: DiceThrow, dice: DieBag, moveConsumer: ((Move) -> Unit)? = null): Ev {
        val remainingDice = dice.size
        val peak = 1 shl remainingDice
        var validKeep = 0L
        for (keepMask in 0 until peak) {
            val keep = thr.mask(keepMask.toByte())
            val gained = keep.multiScore()
            if (gained != Score(0)) {
                val newScore = oldScore + gained
                if (newScore >= limit) {
                    if (moveConsumer != null) {
                        moveConsumer(Move(keepMask.toByte(), false))
                    }
                    return Ev.from(limit)
                }
                validKeep = validKeep or (1L shl keepMask)
            }
        }

        var bestEv = Ev.zero()
        var bestMove: Move? = null

        for (keepMask in 0 until peak) {
            if (((validKeep ushr keepMask) and 1L) == 0L) continue

            val keep = thr.mask(keepMask.toByte())
            val gained = keep.multiScore()
            val newScore = oldScore + gained
            val newScoreEv = Ev.from(newScore)
            if (newScoreEv > bestEv) {
                bestEv = newScoreEv
                if (moveConsumer != null) {
                    bestMove = Move(keepMask.toByte(), false)
                }
            }
            val cont = calculateEvInl(newScore, dice.removeMask(keepMask.toByte()))
            if (cont > bestEv) {
                bestEv = cont
                if (moveConsumer != null) {
                    bestMove = Move(keepMask.toByte(), true)
                }
            }
        }
        if (moveConsumer != null && bestMove != null) {
            moveConsumer(bestMove)
        }
        return bestEv
    }

    data class Move(val keepMask: Byte, val shouldContinue: Boolean)

    @JvmInline
    value class Ev private constructor(private val value: Int) {
        companion object {
            /**
             * Chosen so that `4000 * 6**6 * (1 << SCORE_SHIFT) / SCORE_DIVIDER` fits into 31 bits, so that
             * [WeightedSum] cannot overflow.
             */
            private const val SCORE_SHIFT = 9
            private const val FACTOR = (1 shl SCORE_SHIFT).toDouble() / Score.SCORE_DIVIDER

            internal fun zero() = Ev(0)

            internal fun from(score: Score) = Ev(score.toCompactByte().toInt() shl SCORE_SHIFT)
        }

        internal operator fun compareTo(other: Ev): Int = value.compareTo(other.value)

        fun toDouble(): Double = value.toDouble() / FACTOR
        fun roundToInt(): Int = toDouble().roundToInt()

        @JvmInline
        internal value class Arr private constructor(private val array: IntArray) {
            constructor(size: Int) : this(IntArray(size))

            operator fun set(pos: Int, value: Ev) {
                this.array[pos] = value.value
            }

            operator fun get(i: Int) = Ev(this.array[i])
        }

        @JvmInline
        internal value class WeightedSum private constructor(private val value: Long) {
            fun plus(ev: Ev, weight: Int) = WeightedSum(value + ev.value * weight)
            fun div(totalWeight: Int) = Ev((value / totalWeight).toInt())

            companion object {
                fun zero() = WeightedSum(0)
            }
        }
    }
}