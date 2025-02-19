package at.yawk.kcd2dicesim.worker

import at.yawk.kcd2dicesim.EvProvider
import at.yawk.kcd2dicesim.WasmEvProvider
import kotlinx.serialization.json.Json
import org.w3c.dom.DedicatedWorkerGlobalScope

var provider: EvProvider = EvProvider.Local

fun main() {
    val scope = js("self") as DedicatedWorkerGlobalScope
    WasmEvProvider.INSTANCE.then {
        provider = it
        scope.postMessage(Json.encodeToString<WorkerResponse>(WorkerResponse.WasmLoaded))
    }
    scope.onmessage = { event ->
        val requestJson = event.data as String
        println("worker.onMessage($requestJson)")
        val request = Json.decodeFromString<WorkerRequest>(requestJson)
        when (request) {
            is WorkerRequest.BestEvAndMove -> {
                val response = Json.encodeToString<WorkerResponse>(WorkerResponse.BestEvAndMoveResponse(
                    request.id, provider.bestEvAndMove(request.request)))
                println("worker.bestEvAndMove($requestJson) -> $response")
                scope.postMessage(response)
            }
        }
    }
    println("Worker running")
    scope.postMessage(Json.encodeToString<WorkerResponse>(WorkerResponse.Started))
}