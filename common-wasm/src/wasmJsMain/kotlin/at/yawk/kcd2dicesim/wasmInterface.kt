@file:Suppress("unused")

package at.yawk.kcd2dicesim

import kotlin.wasm.WasmExport

private var move: EvCalculator.Move? = null

@WasmExport
fun wasmCalculateEv(limit: Byte, round: Byte, thr: Int, fullBagLo: Int, fullBagHi: Int, selectedBagLo: Int, selectedBagHi: Int): Double {
    println("wasmCalculateEv($limit, $round, $thr, $fullBagLo, $fullBagHi, $selectedBagLo, $selectedBagHi)")
    val bestEv = EvCalculator(Score.fromCompactByte(limit), DieBag.fromCompactLong(recombineLong(fullBagLo, fullBagHi)))
        .bestEv(Score.fromCompactByte(round), DiceThrow.fromCompactInt(thr), DieBag.fromCompactLong(recombineLong(selectedBagLo, selectedBagHi))) {
            move = it
        }
    return bestEv.toDouble()
}

private fun recombineLong(lo: Int, hi: Int) =
    (lo.toLong() and 0xffffffffL) or (hi.toLong() shl 32)

@WasmExport
fun wasmGetMoveKeepMask(): Byte = move!!.keepMask

@WasmExport
fun wasmGetMoveShouldContinue(): Int = if (move!!.shouldContinue) 1 else 0