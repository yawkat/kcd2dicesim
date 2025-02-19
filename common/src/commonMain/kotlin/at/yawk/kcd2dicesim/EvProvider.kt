package at.yawk.kcd2dicesim

import kotlinx.serialization.Serializable

interface EvProvider {
    fun bestEvAndMove(request: Request): Response

    @Serializable
    data class Request(
        val limit: Score,
        val allDice: DieBag,
        val oldScore: Score,
        val thr: DiceThrow,
        val dice: DieBag
    )

    @Serializable
    data class Response(
        val ev: Double,
        val move: EvCalculator.Move?
    )

    object Local : EvProvider {
        override fun bestEvAndMove(request: Request): Response {
            val (ev, move) = EvCalculator(request.limit, request.allDice).bestEvAndMove(request.oldScore, request.thr, request.dice)
            return Response(ev.toDouble(), move)
        }
    }
}