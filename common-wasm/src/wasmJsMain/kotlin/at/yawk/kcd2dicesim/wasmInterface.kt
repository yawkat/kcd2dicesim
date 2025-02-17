package at.yawk.kcd2dicesim

import kotlin.wasm.WasmExport

private var move: EvCalculator.Move? = null

@WasmExport
fun wasmCalculateEv(limit: Byte, round: Byte, thr: Int, fullBagLo: Int, fullBagHi: Int, selectedBagLo: Int, selectedBagHi: Int): Double {
    println("Thinking in WASM")
    val bestEv = EvCalculator(Score.fromCompactByte(limit), DieBag.fromCompactLong(recombineLong(fullBagLo, fullBagHi)))
        .bestEv(Score.fromCompactByte(round), DiceThrow.fromCompactInt(thr), DieBag.fromCompactLong(recombineLong(selectedBagLo, selectedBagHi))) {
            move = it
        }
    return bestEv
}

private fun recombineLong(lo: Int, hi: Int) =
    (lo.toLong() and 0xffffffffL) or (hi.toLong() shl 32)

@WasmExport
fun wasmGetMoveKeep(): Int = move!!.keep.toCompactInt()

@WasmExport
fun wasmGetMoveShouldContinue(): Boolean = move!!.shouldContinue