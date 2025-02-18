package at.yawk.kcd2dicesim

external fun wasmCalculateEv(limit: Byte, round: Byte, thr: Int, fullBagLo: Int, fullBagHi: Int, selectedBagLo: Int, selectedBagHi: Int): Double

external fun wasmGetMoveKeepMask(): Byte

external fun wasmGetMoveShouldContinue(): Int