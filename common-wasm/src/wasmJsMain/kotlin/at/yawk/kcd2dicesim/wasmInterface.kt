@file:Suppress("unused")

package at.yawk.kcd2dicesim

import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.wasm.WasmExport

@WasmExport
fun wasmBestEvAndMove(writer: Int): Int {
    val request = writers.remove(writer)!!.decodeToString()
    println("wasmCalculateEv($request)")
    val response = Json.encodeToString(EvProvider.Local.bestEvAndMove(Json.decodeFromString(request)))
    println("wasmCalculateEv($request) -> $response")
    return allocate(readers, response.encodeToByteArray())
}

private val writers = mutableMapOf<Int, ByteArray>()
private val readers = mutableMapOf<Int, ByteArray>()

private fun allocate(map: MutableMap<Int, ByteArray>, initialValue: ByteArray): Int {
    while (true) {
        val i = Random.nextInt()
        if (map.containsKey(i)) {
            continue
        }
        map[i] = initialValue
        return i
    }
}

@WasmExport
fun wasmCreateWriter(): Int {
    return allocate(writers, ByteArray(0))
}

@WasmExport
fun wasmWrite(writer: Int, b: Byte) {
    writers[writer] = writers[writer]!! + b
}

@WasmExport
fun wasmRead(reader: Int): Int {
    val arr = readers[reader]!!
    if (arr.isEmpty()) {
        readers.remove(reader)
        return -1
    } else {
        readers[reader] = arr.copyOfRange(1, arr.size)
        return arr[0].toInt() and 0xff
    }
}
