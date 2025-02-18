package at.yawk.kcd2dicesim

import io.kvision.Application
import io.kvision.BootstrapCssModule
import io.kvision.BootstrapModule
import io.kvision.CoreModule
import io.kvision.core.Cursor
import io.kvision.core.onClick
import io.kvision.form.text.text
import io.kvision.html.Button
import io.kvision.html.ButtonStyle
import io.kvision.html.Div
import io.kvision.html.InputType
import io.kvision.html.br
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
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlin.math.roundToInt

private const val NUM_DICE = 6

private fun ObservableValue<Int?>.toStringValue() =
    map({ it?.toString() ?: "" }, { it.toIntOrNull() })

private val wasmAvailable = ObservableValue(false)

class App : Application() {

    private val goal = ObservableValue<Int?>(3000)
    private val scored = ObservableValue<Int?>(0)
    private val round = ObservableValue<Int?>(0)
    private val diceColumns = Array(NUM_DICE) { DieColumn() }

    override fun start() {
        for ((i, col) in diceColumns.withIndex()) {
            val storageKey = "die-type-$i"
            localStorage.getItem(storageKey)?.let { name ->
                SpecialDie.SPECIAL_DICE.find { it.name == name }
            }?.let {
                col.die.value = it
            }
            col.die.subscribe {
                if (it == SpecialDie.NORMAL_DIE) {
                    localStorage.removeItem(storageKey)
                } else {
                    localStorage.setItem(storageKey, it.name)
                }
            }
        }

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
                button("?") {
                    style = ButtonStyle.SECONDARY
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
                button("Bust") {
                    style = ButtonStyle.DANGER
                    onClick {
                        round.value = 0
                    }
                }
            }
            div(className = "row") {
                for (col in diceColumns) {
                    col.element = div(className = "col d-grid gap-1 g-1 pt-1 pb-1") {
                        val buttons = (0..6).map { j ->
                            button(if (j == 0) "/" else "$j") {
                                onClick {
                                    col.value.value = if (j == 0) null else (j - 1).toByte()
                                }
                            }
                        }
                        col.value.subscribe { v ->
                            val selected = (v ?: -1) + 1
                            for ((i, button) in buttons.withIndex()) {
                                if (i == selected) {
                                    button.style = ButtonStyle.PRIMARY
                                } else {
                                    button.style = ButtonStyle.SECONDARY
                                }
                            }
                        }
                        button("") {
                            style = ButtonStyle.OUTLINESECONDARY
                            col.die.subscribe {
                                if (it == SpecialDie.NORMAL_DIE) {
                                    text = "."
                                    removeCssClass("fw-bold")
                                } else {
                                    addCssClass("fw-bold")
                                    text = it.shortName
                                }
                            }
                            onClick {
                                diceSelector(col.die)
                            }
                        }
                    }
                }
            }
            div(className = "row gap-2 mt-2") {
                val think = button("Think", className = "btn col")
                val moveObs = ObservableValue<EvCalculator.Move?>(null)
                var moveScore = Score(0)
                val evDisplay = span(className = "col")
                think.onClick {
                    val workColumns = diceColumns
                        .filter { it.value.value != null }
                        .sortedBy { it.die.value.id }
                    val thr = DiceThrow(*workColumns.mapNotNull { it.value.value }.toByteArray())
                    val bagItems = workColumns.map { it.die.value }
                    val bag = DieBag.of(bagItems)
                    require(bag.toList() == bagItems) { "$bag $bagItems" } // should be ensured by the sort above, but double-check
                    val (bestEv, move) = think(
                        limit = Score((goal.value ?: 3000) - (scored.value ?: 0)),
                        round = Score(round.value ?: 0),
                        thr = thr,
                        fullBag = DieBag.of(diceColumns.map { it.die.value }),
                        selectedBag = bag
                    )
                    moveObs.value = move
                    evDisplay.content =
                        "EV: ${bestEv.roundToInt()} on ${if (move?.shouldContinue == true) "continue" else "PASS"}"
                    if (move != null) {
                        moveScore = thr.mask(move.keepMask).multiScore()
                        for ((i, col) in workColumns.withIndex()) {
                            if (((move.keepMask.toInt() ushr i) and 1) != 0) {
                                col.element.addCssClass("bg-success-subtle")
                            } else {
                                col.element.removeCssClass("bg-success-subtle")
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
                            round.value = (round.value ?: 0) + moveScore.toInt()
                            if (!move.shouldContinue) {
                                scored.value = (scored.value ?: 0) + (round.value ?: 0)
                                round.value = 0
                            }
                            for (col in diceColumns) {
                                col.value.value = null
                                col.element.removeCssClass("bg-success-subtle")
                            }
                            evDisplay.content = ""
                            moveObs.value = null
                        }
                    }
                }
            }
            if (window.location.host != "kcd2dicesim.yawk.at") {
                div(
                    className = "alert alert-info mt-2",
                    content = "The latest version of this tool is always available at <a href='https://kcd2dicesim.yawk.at/'>kcd2dicesim.yawk.at</a>.",
                    rich = true
                )
            }
            div(
                className = "alert alert-warning mt-2",
                content = "Failed to load WebAssembly backend. The application will still work but will be very slow."
            ) {
                wasmAvailable.subscribe {
                    if (it) {
                        parent?.remove(this)
                    }
                }
                if (window.location.protocol.startsWith("file", ignoreCase = true)) {
                    content += " This is likely because you're using a local copy (WebAssembly is not allowed locally) – you can try the website above instead."
                }
            }
        }
    }

    private fun sanitizeForSearch(s: String): String =
        s.lowercase().trim().replace(" ", "").replace("'", "")

    private fun diceSelector(selection: ObservableValue<SpecialDie>) {
        Modal("Die Selection") {
            val search = ObservableValue("")
            text(label = "Search").bindTo(search)
            for (die in SpecialDie.SPECIAL_DICE) {
                p(className = "alert") {
                    span(die.shortName, className = "badge text-bg-secondary")
                    span(" " + die.name)
                    if (die.devilsHead) {
                        br()
                        span("Note: Devil's Head is not yet implemented", className = "fst-italic")
                    }
                    cursor = Cursor.POINTER
                    selection.subscribe {
                        if (selection.value == die) {
                            removeCssClass("alert-secondary")
                            addCssClass("alert-primary")
                        } else {
                            removeCssClass("alert-primary")
                            addCssClass("alert-secondary")
                        }
                    }
                    search.subscribe {
                        visible = sanitizeForSearch(die.name).contains(sanitizeForSearch(it))
                    }
                    onClick {
                        selection.value = die
                        this@Modal.hide()
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

    class DieColumn {
        val value = ObservableValue<Byte?>(null)
        val die = ObservableValue(SpecialDie.NORMAL_DIE)
        lateinit var element: Div
    }
}

private fun think(
    limit: Score,
    round: Score,
    thr: DiceThrow,
    fullBag: DieBag,
    selectedBag: DieBag
): Pair<Double, EvCalculator.Move?> {
    if (wasmAvailable.value) {
        val bestEv = wasmCalculateEv(
            limit = limit.toCompactByte(),
            round = round.toCompactByte(),
            thr = thr.toCompactInt(),
            fullBagLo = fullBag.toCompactLong().toInt(),
            fullBagHi = (fullBag.toCompactLong() ushr 32).toInt(),
            selectedBagLo = selectedBag.toCompactLong().toInt(),
            selectedBagHi = (selectedBag.toCompactLong() ushr 32).toInt()
        )
        val shouldContinue = wasmGetMoveShouldContinue() != 0
        return bestEv to EvCalculator.Move(wasmGetMoveKeepMask(), shouldContinue)
    } else {
        var move: EvCalculator.Move? = null
        val bestEv = EvCalculator(limit, fullBag)
            .bestEv(round, thr, selectedBag) {
                move = it
            }
        return bestEv.toDouble() to move
    }
}

fun main() {
    js("window").onWasmAvailable = { -> wasmAvailable.value = true }
    startApplication(::App, module.hot, BootstrapModule, BootstrapCssModule, CoreModule)
}