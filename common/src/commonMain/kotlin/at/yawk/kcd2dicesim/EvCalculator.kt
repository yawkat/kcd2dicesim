package at.yawk.kcd2dicesim

class EvCalculator(private val limit: Score, private val allDice: DieBag) {
    private val cache = mutableMapOf<Long, Double>()

    fun calculateEv(oldScore: Score, remainingDice: DieBag): Double {
        val i = (oldScore.toCompactByte().toLong() and 0xffL) or (remainingDice.toCompactLong() shl 8)
        val existing = cache[i]
        if (existing == null) {
            val result = calculateEv0(oldScore, remainingDice)
            cache[i] = result
            return result
        } else {
            return existing
        }
    }

    private fun calculateEv0(oldScore: Score, remainingDice: DieBag): Double {
        val n = remainingDice.size
        if (n == 0) {
            return calculateEv(oldScore, allDice)
        }
        var totalEv = 0.0
        DiceThrow.forEachThrow(n) { thr ->
            var weight = 1
            for (i in 0 until n) {
                weight *= remainingDice[i].weights[thr[i].toInt()].toInt()
            }
            if (weight != 0) {
                totalEv += bestEv(oldScore, thr, remainingDice) * weight
            }
        }
        var totalWeight = 1
        for (i in 0 until n) {
            totalWeight *= remainingDice[i].totalWeight
        }
        return totalEv / totalWeight
    }

    fun bestEv(oldScore: Score, thr: DiceThrow, dice: DieBag, moveConsumer: ((Move) -> Unit)? = null): Double {
        val remainingDice = dice.size
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
                    val cont = calculateEv(newScore, dice.removeMask(keepMask.toByte()))
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