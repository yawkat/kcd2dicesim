package at.yawk.kcd2dicesim

import kotlinx.serialization.json.Json
import kotlin.js.Promise

private external fun wasmBestEvAndMove(writer: Int): Int
private external fun wasmCreateWriter(): Int
private external fun wasmWrite(writer: Int, b: Byte)
private external fun wasmRead(reader: Int): Int

private val PROMISE = Promise<EvProvider> { resolve, _ ->
    js("self").onWasmAvailable = { -> resolve(WasmEvProvider()) }
}

private fun write(arr: ByteArray): Int {
    val writer = wasmCreateWriter()
    for (b in arr) {
        wasmWrite(writer, b)
    }
    return writer
}

private fun read(reader: Int): ByteArray {
    var data = ByteArray(0)
    while (true) {
        val b = wasmRead(reader)
        if (b < 0) break
        data += b.toByte()
    }
    return data
}

class WasmEvProvider internal constructor() : EvProvider {
    override fun bestEvAndMove(request: EvProvider.Request): EvProvider.Response {
        val requestJson = Json.encodeToString(request).encodeToByteArray()
        val responseJson = read(wasmBestEvAndMove(write(requestJson)))
        return Json.decodeFromString(responseJson.decodeToString())
    }

    companion object {
        init {
            // eval here so that webpack doesn't try to resolve the shim
            eval("import('./wasm-shim.js')")
        }

        val INSTANCE = PROMISE
    }
}
