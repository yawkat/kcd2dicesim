package at.yawk.kcd2dicesim

class SpecialDie private constructor(
    val shortName: String,
    val name: String,
    val weights: ByteArray,
    val devilsHead: Boolean = false
) {
    init {
        require(weights.size == if (devilsHead) 7 else 6)
        require(weights.all { it < (1 shl BITS_PER_WEIGHT) })
    }

    var id: Byte = 0
        private set

    val totalWeight = weights.sum()

    private val weightsCompact: Int

    init {
        var weightsCompact = 0
        for (w in weights.reversedArray()) {
            weightsCompact = (weightsCompact shl BITS_PER_WEIGHT) or w.toInt()
        }
        this.weightsCompact = weightsCompact
    }

    fun getWeight(eyes: Byte): Byte {
        return ((weightsCompact ushr (BITS_PER_WEIGHT * eyes)) and ((1 shl BITS_PER_WEIGHT) - 1)).toByte()
    }

    override fun toString() = "{$name}"

    private constructor(shortName: String, name: String, vararg weights: Byte) : this(shortName, name, weights)

    companion object {
        private const val BITS_PER_WEIGHT = 4

        /**
         * bit set so that `(JOKER_DIE_SET >> id) & 1 == devilsHead`. This is const so it's statically set, but it's
         * verified by a test.
         */
        const val JOKER_DIE_SET = 0x4000010L

        val NORMAL_DIE = SpecialDie(".", "Normal die", 1, 1, 1, 1, 1, 1)
        val SPECIAL_DICE = listOf(
            NORMAL_DIE,
            SpecialDie("Ar", "Aranka's die", 6, 1, 6, 1, 6, 1),
            SpecialDie("Cc", "Cautious cheater's die", 5, 3, 2, 3, 5, 3),
            SpecialDie("Ci", "Ci die", 3, 3, 3, 3, 3, 8),
            SpecialDie("Dh", "Devil's head die", byteArrayOf(0, 1, 1, 1, 1, 1, 1), true),
            SpecialDie("Dm", "Die of misfortune", 1, 5, 5, 5, 5, 1),
            SpecialDie("Ev", "Even die", 2, 8, 2, 8, 2, 8),
            SpecialDie("Fa", "Favourable die", 6, 0, 1, 1, 6, 4),
            SpecialDie("Fe", "Fer die", 3, 3, 3, 3, 3, 8),
            SpecialDie("Ge", "Greasy die", 3, 2, 3, 2, 3, 4),
            SpecialDie("Gi", "Grimy die", 1, 5, 1, 1, 7, 1),
            SpecialDie("Go", "Grozav's lucky die", 1, 10, 1, 1, 1, 1),
            SpecialDie("Hk", "Heavenly Kingdom die", 7, 2, 2, 2, 2, 4),
            SpecialDie("Ht", "Holy Trinity die", 4, 5, 10, 1, 1, 1),
            SpecialDie("Hu", "Hugo's Die", 1, 1, 1, 1, 1, 1),
            SpecialDie("Ki", "King's die", 4, 6, 7, 8, 4, 3),
            SpecialDie("Lg", "Lousy gambler's die", 2, 3, 2, 3, 7, 3),
            SpecialDie("Lu", "Lu die", 3, 3, 3, 3, 3, 8),
            SpecialDie("Lk", "Lucky Die", 6, 1, 2, 3, 4, 6),
            SpecialDie("Mm", "Mathematician's Die", 4, 5, 6, 7, 1, 1),
            SpecialDie("Mo", "Molar die", 1, 1, 1, 1, 1, 1),
            SpecialDie("Od", "Odd die", 8, 2, 8, 2, 8, 2),
            SpecialDie("Or", "Ordinary die", 1, 1, 1, 1, 1, 1),
            SpecialDie("Pa", "Painted die", 3, 1, 1, 1, 7, 3),
            SpecialDie("Pi", "Pie die", 6, 1, 3, 3, 0, 0),
            SpecialDie("Pr", "Premolar die", 1, 1, 1, 1, 1, 1),
            SpecialDie("Sg", "Sad Greaser's Die", byteArrayOf(0, 6, 1, 1, 6, 3, 6), devilsHead = true),
            SpecialDie("Sa", "Saint Antiochus' die", 0, 0, 9, 0, 0, 0),
            SpecialDie("Sh", "Shrinking die", 2, 1, 1, 1, 1, 3),
            SpecialDie("Ss", "St. Stephen's die", 1, 1, 1, 1, 1, 1),
            SpecialDie("Sr", "Strip die", 4, 2, 2, 2, 3, 3),
            SpecialDie("T3", "Three die", 2, 1, 9, 1, 2, 1),
            SpecialDie("Ub", "Unbalanced Die", 3, 4, 1, 1, 2, 1),
            SpecialDie("Ul", "Unlucky die", 1, 3, 2, 2, 2, 1),
            SpecialDie("Wg", "Wagoner's Die", 1, 5, 6, 2, 2, 2),
            SpecialDie("We", "Weighted die", 10, 1, 1, 1, 1, 1),
            SpecialDie("Wi", "Wisdom tooth die", 1, 1, 1, 1, 1, 1),
        )

        init {
            for ((i, die) in SPECIAL_DICE.withIndex()) {
                die.id = i.toByte()
            }
        }
    }
}