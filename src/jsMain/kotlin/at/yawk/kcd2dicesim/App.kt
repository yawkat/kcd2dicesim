package at.yawk.kcd2dicesim

import io.kvision.Application
import io.kvision.BootstrapCssModule
import io.kvision.BootstrapModule
import io.kvision.CoreModule
import io.kvision.form.FormHorizontalRatio
import io.kvision.form.text.text
import io.kvision.html.Div
import io.kvision.html.InputType
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.span
import io.kvision.module
import io.kvision.panel.root
import io.kvision.startApplication
import io.kvision.state.ObservableValue
import io.kvision.state.bindTo
import kotlin.math.roundToInt

private const val NUM_DICE = 6

private fun ObservableValue<Score?>.toStringValue() =
    map({ it?.toInt()?.toString() ?: "" }, { it.toIntOrNull()?.let { i -> Score(i) } })

class App : Application() {
    private val goal = ObservableValue<Score?>(Score(3000))
    private val scored = ObservableValue<Score?>(Score(0))
    private val round = ObservableValue<Score?>(Score(0))
    private val dice = Array<ObservableValue<Byte?>>(NUM_DICE) { ObservableValue(null) }

    override fun start(state: Map<String, Any>) {
        state["goal"]?.let { if (it != -1) goal.setState(Score(it as Int)) }
        state["round"]?.let { if (it != -1) round.setState(Score(it as Int)) }
        root("kcd2dicesim") {
            addCssClass("container")
            text(InputType.NUMBER, label = "Goal") {
                addCssClass("input-group")
                flabel.addCssClass("input-group-text")
                flabel.removeCssClass("form-label")
                bindTo(goal.toStringValue())
            }
            text(InputType.NUMBER, label = "Scored") {
                addCssClass("input-group")
                flabel.addCssClass("input-group-text")
                flabel.removeCssClass("form-label")
                bindTo(scored.toStringValue())
            }
            text(InputType.NUMBER, label = "Round") {
                addCssClass("input-group")
                flabel.addCssClass("input-group-text")
                flabel.removeCssClass("form-label")
                bindTo(round.toStringValue())
                button("Bust", className = "btn btn-danger") {
                    onClick {
                        round.value = Score(0)
                    }
                }
            }
            lateinit var diceColumns: List<Div>
            div(className = "row") {
                diceColumns = dice.map { obs ->
                    div(className = "col d-grid gap-2 pt-2 pb-2") {
                        val buttons = (0..6).map { j ->
                            button(if (j == 0) "/" else "$j") {
                                addCssClass("btn")
                                onClick {
                                    obs.value = if (j == 0) null else (j - 1).toByte()
                                }
                            }
                        }
                        obs.subscribe { v ->
                            val selected = (v ?: -1) + 1
                            for ((i, button) in buttons.withIndex()) {
                                if (i == selected) {
                                    button.removeCssClass("btn-secondary")
                                    button.addCssClass("btn-primary")
                                } else {
                                    button.addCssClass("btn-secondary")
                                    button.removeCssClass("btn-primary")
                                }
                            }
                        }
                    }
                }
            }
            div(className = "row gap-2 mt-2") {
                val think = button("Think", className = "btn col")
                val moveObs = ObservableValue<EvCalculator.Move?>(null)
                val evDisplay = span(className = "col")
                think.onClick {
                    val bestEv = EvCalculator((goal.value ?: Score(3000)) - (scored.value ?: Score(0)))
                        .bestEv(round.value ?: Score(0), DiceThrow(*dice.mapNotNull { it.value }.toByteArray())) {
                            moveObs.value = it
                        }
                    val move = moveObs.value
                    evDisplay.content = "EV: ${bestEv.roundToInt()} on ${if (move?.shouldContinue == true) "continue" else "PASS"}"
                    if (move != null) {
                        val remainingMoves = move.keep.toArray().toMutableList()
                        for ((i, die) in dice.withIndex()) {
                            val col = diceColumns[i]
                            if (remainingMoves.remove(die.value)) {
                                col.addCssClass("bg-success-subtle")
                            } else {
                                col.removeCssClass("bg-success-subtle")
                            }
                        }
                    }
                }
                button("Accept", className = "btn col") {
                    moveObs.subscribe {
                        disabled = it == null
                    }
                    onClick {
                        val move = moveObs.value
                        if (move != null) {
                            round.value = (round.value ?: Score(0)) + move.keep.multiScore()
                            if (!move.shouldContinue) {
                                scored.value = (scored.value ?: Score(0)) + (round.value ?: Score(0))
                                round.value = Score(0)
                            }
                            for (obs in dice) {
                                obs.value = null
                            }
                            evDisplay.content = ""
                            moveObs.value = null
                        }
                    }
                }
            }
        }
    }

    override fun dispose(): Map<String, Any> {
        console.log("Dispos2")
        return mapOf(
            "goal" to (goal.getState()?.toInt() ?: -1),
            "scored" to (scored.getState()?.toInt() ?: -1),
            "round" to (round.getState()?.toInt() ?: -1),
        )
    }
}

fun main() {
    startApplication(::App, module.hot, BootstrapModule, BootstrapCssModule, CoreModule)
}