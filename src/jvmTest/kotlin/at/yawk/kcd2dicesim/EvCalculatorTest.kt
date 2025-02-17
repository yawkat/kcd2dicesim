package at.yawk.kcd2dicesim

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class EvCalculatorTest {
    @Test
    fun `stability test`() {
        // this test watches for breaking changes in the algorithm
        val calculator = EvCalculator(Score(3000))
        val ev = calculator.bestEv(
            Score(0),
            DiceThrow(0, 1, 1, 2, 2, 3)
        )
        Assertions.assertEquals(427.34382841709623, ev)
    }
}