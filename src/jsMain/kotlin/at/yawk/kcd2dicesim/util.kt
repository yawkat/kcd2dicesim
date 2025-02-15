package at.yawk.kcd2dicesim

import io.kvision.state.MutableState

inline fun <A, B> MutableState<A>.map(crossinline toB: (A) -> B, crossinline toA: (B) -> A): MutableState<B> {
    return object : MutableState<B> {
        override fun getState() = toB(this@map.getState())

        override fun subscribe(observer: (B) -> Unit): () -> Unit {
            return this@map.subscribe {
                observer(toB(it))
            }
        }

        override fun setState(state: B) {
            this@map.setState(toA(state))
        }
    }
}