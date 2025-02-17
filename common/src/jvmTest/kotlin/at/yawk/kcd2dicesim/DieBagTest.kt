package at.yawk.kcd2dicesim

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DieBagTest {
    @Test
    fun `large enough`() {
        Assertions.assertTrue(SpecialDie.SPECIAL_DICE.size < DieBag.DIE_MASK)
    }

    @Test
    fun size() {
        val s5 = SpecialDie.SPECIAL_DICE[5]
        val bag = DieBag.of(listOf(SpecialDie.NORMAL_DIE, SpecialDie.NORMAL_DIE, s5))
        Assertions.assertEquals(3, bag.size)
    }

    @Test
    fun toList() {
        val s5 = SpecialDie.SPECIAL_DICE[5]
        val l = listOf(SpecialDie.NORMAL_DIE, SpecialDie.NORMAL_DIE, s5)
        val bag = DieBag.of(l)
        Assertions.assertEquals(l, bag.toList())
    }

    @Test
    fun `order does not matter for equality`() {
        val s5 = SpecialDie.SPECIAL_DICE[5]
        Assertions.assertEquals(
            DieBag.of(listOf(SpecialDie.NORMAL_DIE, SpecialDie.NORMAL_DIE, s5)),
            DieBag.of(listOf(SpecialDie.NORMAL_DIE, s5, SpecialDie.NORMAL_DIE))
        )
    }

    @Test
    fun remove() {
        val s5 = SpecialDie.SPECIAL_DICE[5]
        Assertions.assertEquals(
            DieBag.of(listOf(SpecialDie.NORMAL_DIE, s5)),
            DieBag.of(listOf(SpecialDie.NORMAL_DIE, SpecialDie.NORMAL_DIE, s5)).remove(0)
        )
        Assertions.assertEquals(
            DieBag.of(listOf(SpecialDie.NORMAL_DIE, s5)),
            DieBag.of(listOf(SpecialDie.NORMAL_DIE, SpecialDie.NORMAL_DIE, s5)).remove(1)
        )
        Assertions.assertEquals(
            DieBag.of(listOf(SpecialDie.NORMAL_DIE, SpecialDie.NORMAL_DIE)),
            DieBag.of(listOf(SpecialDie.NORMAL_DIE, SpecialDie.NORMAL_DIE, s5)).remove(2)
        )

        Assertions.assertEquals(
            DieBag.of(listOf()),
            DieBag.of(listOf(s5)).remove(0)
        )
    }

    @Test
    fun removeMask() {
        val s5 = SpecialDie.SPECIAL_DICE[5]
        Assertions.assertEquals(
            DieBag.of(listOf(SpecialDie.NORMAL_DIE, s5)),
            DieBag.of(listOf(SpecialDie.NORMAL_DIE, SpecialDie.NORMAL_DIE, s5)).removeMask(0b010)
        )
        Assertions.assertEquals(
            DieBag.of(listOf(SpecialDie.NORMAL_DIE)),
            DieBag.of(listOf(SpecialDie.NORMAL_DIE, SpecialDie.NORMAL_DIE, s5)).removeMask(0b110)
        )
        Assertions.assertEquals(
            DieBag.of(listOf(s5)),
            DieBag.of(listOf(SpecialDie.NORMAL_DIE, SpecialDie.NORMAL_DIE, s5)).removeMask(0b011)
        )
    }
}