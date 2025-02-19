package at.yawk.kcd2dicesim.worker

import at.yawk.kcd2dicesim.EvProvider
import kotlinx.serialization.Serializable

@Serializable
sealed class WorkerRequest {
    @Serializable
    data class BestEvAndMove(val id: Int, val request: EvProvider.Request) : WorkerRequest()
}