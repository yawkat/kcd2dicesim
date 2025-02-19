import {wasmBestEvAndMove, wasmCreateWriter, wasmRead, wasmWrite} from "./kcd2dicesim-common-wasm-wasm-js.mjs";

self.wasmBestEvAndMove = wasmBestEvAndMove;
self.wasmCreateWriter = wasmCreateWriter;
self.wasmWrite = wasmWrite;
self.wasmRead = wasmRead;
self.onWasmAvailable()