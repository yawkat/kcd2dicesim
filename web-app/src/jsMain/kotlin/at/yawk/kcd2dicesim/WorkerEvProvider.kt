package at.yawk.kcd2dicesim

import at.yawk.kcd2dicesim.worker.WorkerRequest
import at.yawk.kcd2dicesim.worker.WorkerResponse
import kotlinx.serialization.json.Json
import org.w3c.dom.MODULE
import org.w3c.dom.Worker
import org.w3c.dom.WorkerOptions
import org.w3c.dom.WorkerType
import kotlin.js.Promise
import kotlin.random.Random


class WorkerEvProvider private constructor(resolve: (WorkerEvProvider) -> Unit) : AsyncEvProvider {
    private val worker: Worker
    private val runningTasks = mutableMapOf<Int, (EvProvider.Response) -> Unit>()
    private lateinit var wasmLoaded: () -> Unit
    private val wasmLoadedPromise = Promise<Unit> { resolve, reject ->
        wasmLoaded = { resolve(Unit) }
    }

    init {
        console.log("Launching worker")
        worker = Worker("worker.bundle.js", WorkerOptions(type = WorkerType.MODULE))
        worker.onmessage = { msg ->
            val response = Json.decodeFromString<WorkerResponse>(msg.data as String)
            when (response) {
                is WorkerResponse.BestEvAndMoveResponse -> runningTasks.remove(response.id)?.invoke(response.response)
                WorkerResponse.WasmLoaded -> wasmLoaded()
                WorkerResponse.Started -> resolve(this@WorkerEvProvider)
            }
        }
    }

    override fun bestEvAndMove(request: EvProvider.Request) = Promise<EvProvider.Response> { resolve, reject ->
        worker.onerror = { err ->
            reject(Exception("Exception in worker: $err"))
        }
        var id: Int
        while (true) {
            id = Random.nextInt()
            if (!runningTasks.containsKey(id)) {
                runningTasks[id] = resolve
                break
            }
        }
        worker.postMessage(Json.encodeToString<WorkerRequest>(WorkerRequest.BestEvAndMove(id, request)))
    }

    override fun loadWasm() = wasmLoadedPromise

    companion object {
        fun load() = Promise<WorkerEvProvider> { resolve, reject ->
            WorkerEvProvider(resolve)
        }
    }
}