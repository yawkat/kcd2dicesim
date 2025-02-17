package at.yawk.kcd2dicesim

class SpecialDie private constructor(
    val name: String,
    val weights: ByteArray,
    val devilsHead: Boolean = false
) {
    init {
        require(weights.size == 6)
    }

    var id: Byte = 0
        private set

    val totalWeight = weights.sum()

    override fun toString() = "{$name}"

    private constructor(name: String, vararg weights: Byte) : this(name, weights)

    companion object {
        val NORMAL_DIE = SpecialDie("Normal die", 1, 1, 1, 1, 1, 1)
        val SPECIAL_DICE = listOf(
            NORMAL_DIE,
            SpecialDie("Aranka's die", 6, 1, 6, 1, 6, 1),
            SpecialDie("Cautious cheater's die", 5, 3, 2, 3, 5, 3),
            SpecialDie("Ci die", 3, 3, 3, 3, 3, 8),
            SpecialDie("Devil's head die", byteArrayOf(1, 1, 1, 1, 1, 1), true),
            SpecialDie("Die of misfortune", 1, 5, 5, 5, 5, 1),
            SpecialDie("Even die", 2, 8, 2, 8, 2, 8),
            SpecialDie("Favourable die", 6, 0, 1, 1, 6, 4),
            SpecialDie("Fer die", 3, 3, 3, 3, 3, 8),
            SpecialDie("Greasy die", 3, 2, 3, 2, 3, 4),
            SpecialDie("Grimy die", 1, 5, 1, 1, 7, 1),
            SpecialDie("Grozav's lucky die", 1, 10, 1, 1, 1, 1),
            SpecialDie("Heavenly Kingdom die", 7, 2, 2, 2, 2, 4),
            SpecialDie("Holy Trinity die", 4, 5, 10, 1, 1, 1),
            SpecialDie("Hugo's Die", 1, 1, 1, 1, 1, 1),
            SpecialDie("King's die", 4, 6, 7, 8, 4, 3),
            SpecialDie("Lousy gambler's die", 2, 3, 2, 3, 7, 3),
            SpecialDie("Lu die", 3, 3, 3, 3, 3, 8),
            SpecialDie("Lucky Die", 6, 1, 2, 3, 4, 6),
            SpecialDie("Mathematician's Die", 4, 5, 6, 7, 1, 1),
            SpecialDie("Molar die", 1, 1, 1, 1, 1, 1),
            SpecialDie("Odd die", 8, 2, 8, 2, 8, 2),
            SpecialDie("Ordinary die", 1, 1, 1, 1, 1, 1),
            SpecialDie("Painted die", 3, 1, 1, 1, 7, 3),
            SpecialDie("Pie die", 6, 1, 3, 3, 0, 0),
            SpecialDie("Premolar die", 1, 1, 1, 1, 1, 1),
            SpecialDie("Sad Greaser's Die", byteArrayOf(6, 6, 1, 1, 6, 3), devilsHead = true),
            SpecialDie("Saint Antiochus' die", 0, 0, 9, 0, 0, 0),
            SpecialDie("Shrinking die", 2, 1, 1, 1, 1, 3),
            SpecialDie("St. Stephen's die", 1, 1, 1, 1, 1, 1),
            SpecialDie("Strip die", 4, 2, 2, 2, 3, 3),
            SpecialDie("Three die", 2, 1, 9, 1, 2, 1),
            SpecialDie("Unbalanced Die", 3, 4, 1, 1, 2, 1),
            SpecialDie("Unlucky die", 1, 3, 2, 2, 2, 1),
            SpecialDie("Wagoner's Die", 1, 5, 6, 2, 2, 2),
            SpecialDie("Weighted die", 10, 1, 1, 1, 1, 1),
            SpecialDie("Wisdom tooth die", 1, 1, 1, 1, 1, 1),
        )

        init {
            for ((i, die) in SPECIAL_DICE.withIndex()) {
                die.id = i.toByte()
            }
        }
    }
}