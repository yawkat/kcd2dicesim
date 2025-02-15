package at.yawk.kcd2dicesim

import io.kvision.Application
import io.kvision.BootstrapCssModule
import io.kvision.BootstrapModule
import io.kvision.CoreModule
import io.kvision.form.text.text
import io.kvision.html.Button
import io.kvision.html.Div
import io.kvision.html.InputType
import io.kvision.html.button
import io.kvision.html.div
import io.kvision.html.li
import io.kvision.html.link
import io.kvision.html.p
import io.kvision.html.span
import io.kvision.html.ul
import io.kvision.modal.Modal
import io.kvision.modal.ModalSize
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
            val help = Modal("Help", size = ModalSize.LARGE) {
                p(
                    "This site simulates the dice minigame in <a href='https://store.steampowered.com/app/1771300/Kingdom_Come_Deliverance_II/'>Kingdom Come: Deliverance II</a>. For a given set of thrown dice, it determines possible moves and recommends those with the best <a href='https://en.wikipedia.org/wiki/Expected_value'>expected value</a> (EV).",
                    rich = true
                )
                p("There are three score fields:")
                ul {
                    li("The 'Goal' field contains the maximum number of points you wish to reach, e.g. 3000 for a Masters-level game.")
                    li("The 'Scored' field contains the number of points you have already 'locked in' / scored. In combination with the goal, this determines how many points you still need. The simulation will consider points beyond this value (goal - scored) worthless.")
                    li("The 'Round' field contains the number of points on dice you've set aside in the current round. If you go bust, these points may disappear.")
                }
                p("Below the score fields is the dice selection. Each column represents one die. After a throw, select the number of eyes for all the dice here. There's six columns, but if a throw contained fewer than six dice, just leave some columns as '/'. Those dice will be ignored.")
                p("Once you've entered all the dice values, press 'Think'. This button will simulate the moves. It may take a few seconds to compute. Once the simulation completes, a suggested move is displayed. The dice to be held get a green background. Next to the 'Think' button, the expected value is displayed, along with whether you should pass or continue with more throws.")
                p("The 'Accept' button applies the suggested throw to the scores. The value of the held dice will be added to the 'Round' score. If the suggestion was to pass, the 'Round' score is added to the 'Scored' score and the inputs are reset for a new round.")
                p {
                    link("Contribute on GitHub", "https://github.com/yawkat/kcd2dicesim")
                }
                addButton(Button("ok") {
                    onClick {
                        this@Modal.hide()
                    }
                })
            }
            text(InputType.NUMBER, label = "Goal") {
                addCssClass("input-group")
                flabel.addCssClass("input-group-text")
                flabel.removeCssClass("form-label")
                bindTo(goal.toStringValue())
                button("?", className = "btn btn-secondary") {
                    onClick {
                        help.show()
                    }
                }
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