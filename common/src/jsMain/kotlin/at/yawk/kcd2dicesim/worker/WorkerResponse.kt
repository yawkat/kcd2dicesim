package at.yawk.kcd2dicesim.worker

import at.yawk.kcd2dicesim.EvProvider
import kotlinx.serialization.Serializable

@Serializable
sealed class WorkerResponse {
    @Serializable
    data class BestEvAndMoveResponse(val id: Int, val response: EvProvider.Response) : WorkerResponse()

    @Serializable
    object WasmLoaded : WorkerResponse()

    @Serializable
    object Started : WorkerResponse()
}