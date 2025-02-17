package at.yawk.kcd2dicesim

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt

class EvCalculatorTest {
    @Test
    fun `stability test`() {
        // this test watches for breaking changes in the algorithm
        val bag = DieBag.of(
            listOf(
                SpecialDie.NORMAL_DIE,
                SpecialDie.NORMAL_DIE,
                SpecialDie.NORMAL_DIE,
                SpecialDie.NORMAL_DIE,
                SpecialDie.NORMAL_DIE,
                SpecialDie.NORMAL_DIE
            )
        )
        val calculator = EvCalculator(Score(3000), bag)
        val ev = calculator.bestEv(
            Score(0),
            DiceThrow(0, 1, 1, 2, 2, 3),
            bag
        )
        Assertions.assertEquals(427.34382841709623, ev)
    }

    @Test
    fun `stability test with special dice`() {
        // this test watches for breaking changes in the algorithm
        val bag = DieBag.of(
            listOf(
                SpecialDie.NORMAL_DIE,
                SpecialDie.NORMAL_DIE,
                SpecialDie.NORMAL_DIE,
                SpecialDie.NORMAL_DIE,
                SpecialDie.NORMAL_DIE,
                SpecialDie.SPECIAL_DICE[1],
            )
        )
        val calculator = EvCalculator(Score(3000), bag)
        val ev = calculator.bestEv(
            Score(0),
            DiceThrow(0, 1, 1, 2, 2, 3),
            bag
        )
        Assertions.assertEquals(502.94831893880513, ev)
    }

    @Test
    fun `best dice`() {
        for (d in SpecialDie.SPECIAL_DICE) {
            val bag = DieBag.of(
                listOf(
                    d,
                    SpecialDie.NORMAL_DIE,
                    SpecialDie.NORMAL_DIE,
                    SpecialDie.NORMAL_DIE,
                    SpecialDie.NORMAL_DIE,
                    SpecialDie.NORMAL_DIE
                )
            )
            val calculator = EvCalculator(Score(3000), bag)
            val ev = calculator.bestEv(
                Score(0),
                DiceThrow(0, 1, 1, 2, 2, 3),
                bag
            )
            println("$d : ${ev.roundToInt()}")
        }
    }
}