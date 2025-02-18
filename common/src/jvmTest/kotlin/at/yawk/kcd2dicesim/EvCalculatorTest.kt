package at.yawk.kcd2dicesim

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

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
        // result with full accuracy double: 427.34382841709623
        Assertions.assertEquals(427.24609375, ev.toDouble())
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
        // result with full accuracy double: 502.94831893880513
        Assertions.assertEquals(502.83203125, ev.toDouble())
    }

    @Test
    @Disabled
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

    @Test
    fun `perf testing`() {
        DiceThrow // initialize

        val start = System.nanoTime()
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
        calculator.calculateEv(Score(0), bag)
        val end = System.nanoTime()
        println("Took ${(end - start) / 1000000.0} ms")
    }
}