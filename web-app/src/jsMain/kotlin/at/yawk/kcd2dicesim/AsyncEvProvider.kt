package at.yawk.kcd2dicesim

import kotlin.js.Promise

interface AsyncEvProvider {
    fun bestEvAndMove(request: EvProvider.Request): Promise<EvProvider.Response>

    fun loadWasm(): Promise<*>

    class Local : AsyncEvProvider {
        private var sync: EvProvider = EvProvider.Local

        override fun bestEvAndMove(request: EvProvider.Request)= Promise<EvProvider.Response> { resolve, reject ->
            try {
                resolve(sync.bestEvAndMove(request))
            } catch (e: Exception) {
                reject(e)
            }
        }

        override fun loadWasm(): Promise<Unit> {
            return WasmEvProvider.INSTANCE.then { pr ->
                sync = pr
            }
        }
    }
}