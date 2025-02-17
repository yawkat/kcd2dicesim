package at.yawk.kcd2dicesim

@JsModule("k2dicesim-wasm-js")
@JsNonModule
external fun wasmCalculateEv(limit: Byte, round: Byte, thr: Int, fullBagLo: Int, fullBagHi: Int, selectedBagLo: Int, selectedBagHi: Int): Double

@JsModule("k2dicesim-wasm-js")
@JsNonModule
external fun wasmGetMoveKeep(): Int

@JsModule("k2dicesim-wasm-js")
@JsNonModule
external fun wasmGetMoveShouldContinue(): Boolean