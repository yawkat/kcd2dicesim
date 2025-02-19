package at.yawk.kcd2dicesim

import at.yawk.kcd2dicesim.DieBag.Companion.BITS_PER_DIE
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * *Unordered* collection of [SpecialDie].
 *
 * Packing: [BITS_PER_DIE] bits per die. The bits are [SpecialDie.id].
 */
@JvmInline
@Serializable
value class DieBag private constructor(private val value: Long) {
    internal val size: Int
        inline get() = value.toInt() and ((1 shl LENGTH_BITS) - 1)

    internal val onlyNormalDice: Boolean
        inline get() = size.toLong() == value // all die IDs zero

    val jokerMask: Byte
        get() {
            var mask = 0
            for (i in 0 until size) {
                mask = mask or (((SpecialDie.JOKER_DIE_SET ushr getId(i)).toInt() and 1) shl i)
            }
            return mask.toByte()
        }

    operator fun get(index: Int) = SpecialDie.SPECIAL_DICE[getId(index)]

    private fun getId(index: Int): Int = (value ushr (BITS_PER_DIE * index + LENGTH_BITS)).toInt() and DIE_MASK.toInt()

    fun remove(index: Int): DieBag {
        return removeMask((1 shl index).toByte())
    }

    fun removeMask(mask: Byte): DieBag {
        if (onlyNormalDice) {
            return DieBag(value - mask.countOneBits())
        }

        var modified = value
        var dieMask = dieMask(size - 1)
        for (i in size - 1 downTo 0) {
            if ((mask.toInt() and (1 shl i)) != 0) {
                val lo = modified and dieMask
                val hi = (modified ushr BITS_PER_DIE) and dieMask.inv()
                modified = (lo or hi) - 1 // -1 to fix size
            }
            dieMask = dieMask ushr BITS_PER_DIE
        }
        return DieBag(modified)
    }

    fun toList(): List<SpecialDie> = (0 until size).map { get(it) }

    fun toCompactLong(): Long = value

    override fun toString() = "DieBag${toList()}"

    companion object {
        private const val LENGTH_BITS = 3
        private const val BITS_PER_DIE = 6

        const val DIE_MASK = ((1 shl BITS_PER_DIE) - 1).toByte()

        private fun dieMask(n: Int): Long = (1L shl (BITS_PER_DIE * n + LENGTH_BITS)) - 1

        fun of(dice: Collection<SpecialDie>): DieBag {
            var value = 0L
            for (die in dice.sortedBy { -it.id }) {
                value = (value shl BITS_PER_DIE) or die.id.toLong()
            }
            value = (value shl LENGTH_BITS) or dice.size.toLong()
            return DieBag(value)
        }

        fun fromCompactLong(long: Long) = DieBag(long)
    }
}