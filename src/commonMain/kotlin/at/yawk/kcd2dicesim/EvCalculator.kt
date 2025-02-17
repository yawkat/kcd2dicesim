package at.yawk.kcd2dicesim

class EvCalculator(private val limit: Score) {
    private val cache = DoubleArray(7 * (limit.toCompactByte() + 1)) { Double.NaN }

    fun calculateEv(oldScore: Score, remainingDice: Int): Double {
        val i = oldScore.toCompactByte() * 7 + remainingDice
        val existing = cache[i]
        if (existing.isNaN()) {
            val result = calculateEv0(oldScore, remainingDice)
            cache[i] = result
            return result
        } else {
            return existing
        }
    }

    private fun calculateEv0(oldScore: Score, remainingDice: Int): Double {
        if (remainingDice == 0) {
            return calculateEv(oldScore, 6)
        }
        var totalEv = 0.0
        for (i in 0 until COMBINATION_COUNTS[remainingDice]) {
            val thr = DiceThrow(i, remainingDice)
            totalEv += bestEv(oldScore, thr)
        }
        return totalEv / COMBINATION_COUNTS[remainingDice]
    }

    fun bestEv(oldScore: Score, thr: DiceThrow, moveConsumer: ((Move) -> Unit)? = null): Double {
        val remainingDice = thr.length
        val peak = 1 shl remainingDice
        var bestEv = 0.0
        var bestMove: Move? = null
        for (keepMask in 0 until peak) {
            val keep = thr.mask(keepMask.toByte())
            val gained = keep.multiScore()
            if (gained != Score(0)) {
                val newScore = oldScore + gained
                if (newScore > limit) {
                    bestEv = limit.toInt().toDouble()
                    if (moveConsumer != null) {
                        bestMove = Move(keep, false)
                    }
                    break
                } else {
                    if (newScore.toInt() > bestEv) {
                        bestEv = newScore.toInt().toDouble()
                        if (moveConsumer != null) {
                            bestMove = Move(keep, false)
                        }
                    }
                    val cont = calculateEv(newScore, remainingDice - keep.length)
                    if (cont > bestEv) {
                        bestEv = cont
                        if (moveConsumer != null) {
                            bestMove = Move(keep, true)
                        }
                    }
                }
            }
        }
        if (moveConsumer != null && bestMove != null) {
            moveConsumer(bestMove)
        }
        return bestEv
    }

    data class Move(val keep: DiceThrow, val shouldContinue: Boolean)
}