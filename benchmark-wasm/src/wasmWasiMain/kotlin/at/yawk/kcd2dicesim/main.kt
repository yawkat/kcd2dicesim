package at.yawk.kcd2dicesim

private fun recombineLong(lo: Int, hi: Int) =
    (lo.toLong() and 0xffffffffL) or (hi.toLong() shl 32)
private var move: EvCalculator.Move? = null

fun wasmCalculateEv(limit: Byte, round: Byte, thr: Int, fullBagLo: Int, fullBagHi: Int, selectedBagLo: Int, selectedBagHi: Int): Double {
    println("wasmCalculateEv($limit, $round, $thr, $fullBagLo, $fullBagHi, $selectedBagLo, $selectedBagHi)")
    val bestEv = EvCalculator(Score.fromCompactByte(limit), DieBag.fromCompactLong(recombineLong(fullBagLo, fullBagHi)))
        .bestEv(Score.fromCompactByte(round), DiceThrow.fromCompactInt(thr), DieBag.fromCompactLong(recombineLong(selectedBagLo, selectedBagHi))) {
            move = it
        }
    return bestEv.toDouble()
}

fun main() {
    println("Running...")
    wasmCalculateEv(60, 0, 145041, 6, 0, 6, 0)
    println("Done!")
}