package at.yawk.kcd2dicesim

import io.kvision.Application
import io.kvision.BootstrapCssModule
import io.kvision.BootstrapModule
import io.kvision.CoreModule
import io.kvision.core.onClick
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
import kotlinx.browser.window
import kotlin.math.roundToInt

private const val NUM_DICE = 6

private fun ObservableValue<Int?>.toStringValue() =
    map({ it?.toString() ?: "" }, { it.toIntOrNull() })

class App : Application() {
    private val goal = ObservableValue<Int?>(3000)
    private val scored = ObservableValue<Int?>(0)
    private val round = ObservableValue<Int?>(0)
    private val diceValues = Array<ObservableValue<Byte?>>(NUM_DICE) { ObservableValue(null) }
    private val dice = Array(NUM_DICE) { ObservableValue(SpecialDie.NORMAL_DIE) }

    override fun start(state: Map<String, Any>) {
        state["goal"]?.let { if (it != -1) goal.setState(it as Int) }
        state["round"]?.let { if (it != -1) round.setState(it as Int) }
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
                        round.value = 0
                    }
                }
            }
            lateinit var diceColumns: List<Div>
            div(className = "row") {
                diceColumns = diceValues.mapIndexed { diceIndex, obs ->
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
                        val diceChoice = dice[diceIndex]
                        button("", className = "btn btn-link") {
                            diceChoice.subscribe {
                                text = it.name.substring(0, 1) // TODO
                            }
                            onClick {
                                diceSelector(diceChoice)
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
                    val typeAndValue = (0 until NUM_DICE)
                        .map { dice[it].value to diceValues[it].value }
                        .filter { it.second != null }
                        .sortedBy { it.first.id }
                    val thr = DiceThrow(*typeAndValue.mapNotNull { it.second }.toByteArray())
                    val bagItems = typeAndValue.map { it.first }
                    val bag = DieBag.of(bagItems)
                    require(bag.toList() == bagItems) { "$bag $bagItems" } // should be ensured by the sort above, but double-check
                    val (bestEv, move) = think(
                        limit = Score((goal.value ?: 3000) - (scored.value ?: 0)),
                        round = Score(round.value ?: 0),
                        thr = thr,
                        fullBag = DieBag.of(dice.map { it.value }),
                        selectedBag = bag
                    )
                    moveObs.value = move
                    evDisplay.content = "EV: ${bestEv.roundToInt()} on ${if (move?.shouldContinue == true) "continue" else "PASS"}"
                    if (move != null) {
                        val remainingMoves = move.keep.toArray().toMutableList()
                        for ((i, die) in diceValues.withIndex()) {
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
                            round.value = (round.value ?: 0) + move.keep.multiScore().toInt()
                            if (!move.shouldContinue) {
                                scored.value = (scored.value ?: 0) + (round.value ?: 0)
                                round.value = 0
                            }
                            for (obs in diceValues) {
                                obs.value = null
                            }
                            for (column in diceColumns) {
                                column.removeCssClass("bg-success-subtle")
                            }
                            evDisplay.content = ""
                            moveObs.value = null
                        }
                    }
                }
            }
            if (window.location.host != "kcd2dicesim.yawk.at") {
                div(className = "alert alert-info mt-2", content = "The latest version of this tool is always available at <a href='https://kcd2dicesim.yawk.at/'>kcd2dicesim.yawk.at</a>.", rich = true)
            }
        }
    }

    private fun diceSelector(selection: ObservableValue<SpecialDie>) {
        Modal("Dice Selection") {
            for (die in SpecialDie.SPECIAL_DICE) {
                div(className = "alert", content = die.name) {
                    selection.subscribe {
                        if (selection.value == die) {
                            removeCssClass("alert-secondary")
                            addCssClass("alert-primary")
                        } else {
                            removeCssClass("alert-primary")
                            addCssClass("alert-secondary")
                        }
                    }
                    onClick {
                        selection.value = die
                    }
                }
            }
            addButton(Button("ok") {
                onClick {
                    this@Modal.hide()
                }
            })
        }.show()
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

private fun think(limit: Score, round: Score, thr: DiceThrow, fullBag: DieBag, selectedBag: DieBag): Pair<Double, EvCalculator.Move?> {
    if (true) { // TODO
        val bestEv = wasmCalculateEv(
            limit = limit.toCompactByte(),
            round = round.toCompactByte(),
            thr = thr.toCompactInt(),
            fullBagLo = fullBag.toCompactLong().toInt(),
            fullBagHi = (fullBag.toCompactLong() ushr 32).toInt(),
            selectedBagLo = selectedBag.toCompactLong().toInt(),
            selectedBagHi = (selectedBag.toCompactLong() ushr 32).toInt()
        )
        return bestEv to EvCalculator.Move(DiceThrow.fromCompactInt(wasmGetMoveKeep()), wasmGetMoveShouldContinue())
    }

    var move: EvCalculator.Move? = null
    val bestEv = EvCalculator(limit, fullBag)
        .bestEv(round, thr, selectedBag) {
            move = it
        }
    return bestEv to move
}

fun main() {
    startApplication(::App, module.hot, BootstrapModule, BootstrapCssModule, CoreModule)
}