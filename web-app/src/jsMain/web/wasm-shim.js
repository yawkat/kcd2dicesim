import {wasmCalculateEv, wasmGetMoveKeepMask, wasmGetMoveShouldContinue} from "./kcd2dicesim-common-wasm-wasm-js.mjs";

window.wasmCalculateEv = wasmCalculateEv;
window.wasmGetMoveKeepMask = wasmGetMoveKeepMask;
window.wasmGetMoveShouldContinue = wasmGetMoveShouldContinue;
window.onWasmAvailable()