package at.yawk.kcd2dicesim

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SpecialDieTest {
    @Test
    fun `check compact weights`() {
        for (die in SpecialDie.SPECIAL_DICE) {
            Assertions.assertArrayEquals(
                die.weights,
                (0..5).map { die.getWeight(it.toByte()) }.toByteArray()
            )
        }
    }

    @Test
    fun `short name unique`() {
        require(SpecialDie.SPECIAL_DICE.map { it.shortName }.toSet().size == SpecialDie.SPECIAL_DICE.size)
    }
}