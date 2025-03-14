package at.yawk.kcd2dicesim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DiceThrowTest {
    @Test
    fun sorted() {
        assertEquals(
            DiceThrow(1, 2, 3, 4, 5),
            DiceThrow(3, 4, 1, 2, 5).sorted()
        )
    }

    @Test
    fun mask() {
        assertEquals(
            DiceThrow(2, 3, 5),
            DiceThrow(1, 2, 3, 4, 5).mask(0b10110)
        )
    }

    @Test
    fun selectionScoreSingle() {
        assertEquals(
            Score(750),
            DiceThrow(1, 2, 5, 3, 4).selectionScoreSingle(),
        )
        assertEquals(
            Score(800),
            DiceThrow(1, 1, 1, 1, 1).selectionScoreSingle(),
        )
    }

    @Test
    fun multiScore() {
        assertEquals(
            Score(750),
            DiceThrow(1, 2, 5, 3, 4).selectionScoreSingle(),
        )
        assertEquals(
            Score(800),
            DiceThrow(1, 1, 1, 1, 1).selectionScoreSingle(),
        )
        assertEquals(
            Score(300),
            DiceThrow(1, 1, 4, 4, 1).multiScore(),
        )
    }

    @Test
    fun joker() {
        assertEquals(
            Score(750),
            DiceThrow(1, 2, 5, 3, JOKER).selectionScoreSingle(),
        )
        assertEquals(
            Score(300),
            DiceThrow(1, JOKER, 4, 4, 1).multiScore(),
        )
    }
}