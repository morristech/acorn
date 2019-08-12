/*
 *    Copyright 2018 Niek Haarman
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.nhaarman.acorn.navigation.experimental

import androidx.annotation.CallSuper
import com.nhaarman.acorn.navigation.DisposableHandle
import com.nhaarman.acorn.navigation.Navigator
import com.nhaarman.acorn.navigation.SavableNavigator
import com.nhaarman.acorn.navigation.TransitionData
import com.nhaarman.acorn.navigation.experimental.internal.v
import com.nhaarman.acorn.navigation.experimental.internal.w
import com.nhaarman.acorn.presentation.Container
import com.nhaarman.acorn.presentation.Scene
import com.nhaarman.acorn.state.NavigatorState
import com.nhaarman.acorn.state.get
import com.nhaarman.acorn.util.lazyVar

@ExperimentalCompositeParallelNavigator
abstract class CompositeParallelNavigator<Destination>(
    private val initialDestination: Destination,
    private val savedState: NavigatorState?
) : Navigator {

    abstract fun serialize(destination: Destination): String
    abstract fun deserialize(serializedDestination: String): Destination

    abstract fun createNavigator(destination: Destination, savedState: NavigatorState?): Navigator

    private fun createNavigatorInternal(destination: Destination, savedState: NavigatorState?): Navigator {
        val result = createNavigator(destination, savedState)
        addListenerTo(result, destination)
        return result
    }

    private var state by lazyVar {

        fun initialState(): LifecycleState<Destination> {
            return LifecycleState.create(
                this,
                initialDestination,
                mapOf(initialDestination to createNavigatorInternal(initialDestination, null))
            )
        }

        if (savedState == null) {
            return@lazyVar initialState()
        }

        val size: Int? = savedState.get("size")
        if (size == null || size == 0) {
            return@lazyVar initialState()
        }

        val map = (0 until size)
            .map { index ->
                val destination: Destination = savedState.get<String>("${index}_destination")?.let(::deserialize)
                    ?: return@lazyVar initialState()

                val state: NavigatorState = savedState["${index}_state"]
                    ?: return@lazyVar initialState()

                val navigator = createNavigatorInternal(destination, state)
                destination to navigator
            }
            .toMap()

        val selectedDestination = savedState.get<String>("selected_destination")?.let(::deserialize)
            ?: return@lazyVar initialState()

        LifecycleState.create(
            this,
            selectedDestination,
            map
        )
    }

    private fun addListenerTo(navigator: Navigator, destination: Destination) {
        // Child navigators have a shorter lifetime than their parent navigators,
        // so it is not necessary to unregister the listener.
        // noinspection CheckResult
        navigator.addNavigatorEventsListener(ChildNavigatorListener(destination))
    }

    override fun addNavigatorEventsListener(listener: Navigator.Events): DisposableHandle {
        state.addListener(listener)

        return object : DisposableHandle {

            override fun isDisposed(): Boolean {
                return listener !in state.listeners
            }

            override fun dispose() {
                state.removeListener(listener)
            }
        }
    }

    fun select(destination: Destination) {
        v(this.javaClass.simpleName, "select $destination")
        execute(state.select(destination))
    }

    override fun onStart() {
        v(this.javaClass.simpleName, "onStart")
        execute(state.start())
    }

    override fun onStop() {
        v(this.javaClass.simpleName, "onStop")
        execute(state.stop())
    }

    override fun onDestroy() {
        v(this.javaClass.simpleName, "onDestroy")
        execute(state.destroy())
    }

    private fun execute(transition: StateTransition<Destination>) {
        state = transition.newState
        transition.action?.invoke()
    }

    @CallSuper
    open fun saveInstanceState(): NavigatorState {
        if (state is Destroyed) {
            return NavigatorState()
        }

        return state.navigators
            .entries
            .foldIndexed(NavigatorState()) { index, state, (destination, navigator) ->
                state.also {
                    it["${index}_destination"] = serialize(destination)
                    it["${index}_state"] = (navigator as? SavableNavigator)?.saveInstanceState()
                }
            }
            .also {
                it["size"] = state.navigators.size
                it["selected_destination"] = serialize(state.selectedDestination)
            }
    }

    override fun isDestroyed(): Boolean {
        return state is Destroyed
    }

    private inner class ChildNavigatorListener(
        private val childDestination: Destination
    ) : Navigator.Events {

        @CallSuper
        override fun scene(scene: Scene<out Container>, data: TransitionData?) {
            v(this@CompositeParallelNavigator.javaClass.simpleName, "Scene change: $scene, $data")
            state.scene(scene, data)
        }

        override fun finished() {
            state.childFinished(childDestination)
        }
    }

    private abstract class LifecycleState<D> {

        abstract val navigators: Map<D, Navigator>
        abstract val selectedDestination: D

        abstract val listeners: List<Navigator.Events>

        abstract fun addListener(listener: Navigator.Events)
        abstract fun removeListener(listener: Navigator.Events)

        abstract fun start(): StateTransition<D>
        abstract fun stop(): StateTransition<D>
        abstract fun destroy(): StateTransition<D>

        abstract fun scene(scene: Scene<out Container>, data: TransitionData?)
        abstract fun childFinished(finishedDestination: D)

        abstract fun select(destination: D): StateTransition<D>

        companion object {

            fun <D> create(
                receiver: CompositeParallelNavigator<D>,
                selectedDestination: D,
                navigators: Map<D, Navigator>
            ): LifecycleState<D> {
                return receiver.Inactive(selectedDestination, navigators.toMutableMap(), emptyList())
            }
        }
    }

    private inner class Inactive(
        override val selectedDestination: Destination,
        override val navigators: MutableMap<Destination, Navigator>,
        override var listeners: List<Navigator.Events>
    ) : LifecycleState<Destination>() {

        init {
            check(navigators.contains(selectedDestination)) {
                "Navigators should contain entry for selected destination ($selectedDestination)."
            }
        }

        override fun addListener(listener: Navigator.Events) {
            listeners += listener
        }

        override fun removeListener(listener: Navigator.Events) {
            listeners -= listener
        }

        override fun start(): StateTransition<Destination> {
            return StateTransition(Active(selectedDestination, navigators, listeners)) {
                navigators[selectedDestination]!!.onStart()
            }
        }

        override fun stop(): StateTransition<Destination> {
            return StateTransition(this)
        }

        override fun destroy(): StateTransition<Destination> {
            return StateTransition(Destroyed()) {
                navigators
                    .values
                    .forEach { it.onDestroy() }
            }
        }

        override fun scene(scene: Scene<out Container>, data: TransitionData?) {
        }

        override fun childFinished(finishedDestination: Destination) {
            navigators.remove(finishedDestination)

            if (finishedDestination == initialDestination) {
                listeners.forEach { it.finished() }
                return
            }

            if (finishedDestination == selectedDestination) {
                this@CompositeParallelNavigator.select(initialDestination)
            }
        }

        override fun select(destination: Destination): StateTransition<Destination> {
            val newNavigators = navigators
                .also { it.getOrPut(destination) { createNavigatorInternal(destination, null) } }

            return StateTransition(
                Inactive(
                    selectedDestination = destination,
                    navigators = newNavigators,
                    listeners = listeners
                )
            )
        }
    }

    private inner class Active(
        override val selectedDestination: Destination,
        override val navigators: MutableMap<Destination, Navigator>,
        override var listeners: List<Navigator.Events>
    ) : LifecycleState<Destination>() {

        private var activeScene: Scene<out Container>? = null

        override fun addListener(listener: Navigator.Events) {
            listeners += listener
            activeScene?.let { listener.scene(it, null) }
        }

        override fun removeListener(listener: Navigator.Events) {
            listeners -= listener
        }

        override fun start(): StateTransition<Destination> {
            return StateTransition(this)
        }

        override fun stop(): StateTransition<Destination> {
            return StateTransition(
                Inactive(
                    selectedDestination,
                    navigators,
                    listeners
                )
            ) {
                navigators[selectedDestination]!!.onStop()
            }
        }

        override fun destroy(): StateTransition<Destination> {
            return stop().andThen { it.destroy() }
        }

        override fun scene(scene: Scene<out Container>, data: TransitionData?) {
            activeScene = scene
            listeners.onEach { it.scene(scene, data) }
        }

        override fun childFinished(finishedDestination: Destination) {
            navigators.remove(finishedDestination)

            if (finishedDestination == initialDestination) {
                listeners.forEach { it.finished() }
                return
            }

            if (finishedDestination == selectedDestination) {
                this@CompositeParallelNavigator.select(initialDestination)
            }
        }

        override fun select(destination: Destination): StateTransition<Destination> {
            if (destination == selectedDestination) return StateTransition(this)

            val newNavigators = navigators
                .also { it.getOrPut(destination) { createNavigatorInternal(destination, null) } }

            return StateTransition(
                Active(
                    destination,
                    newNavigators,
                    listeners
                )
            ) {
                navigators[selectedDestination]?.onStop()
                newNavigators[destination]!!.onStart()
            }
        }
    }

    private inner class Destroyed : LifecycleState<Destination>() {

        override val selectedDestination: Destination
            get() = error("A destroyed Navigator doesn't have a selected destination.")

        override val navigators: Map<Destination, Navigator>
            get() = emptyMap()

        override val listeners: List<Navigator.Events>
            get() = emptyList()

        override fun addListener(listener: Navigator.Events) {
            w(
                "CompositeParallelNavigator.LifecycleState",
                "Warning: Ignoring listener for destroyed navigator."
            )
        }

        override fun removeListener(listener: Navigator.Events) {
        }

        override fun start(): StateTransition<Destination> {
            w("CompositeParallelNavigator.LifecycleState", "Warning: Cannot start state after navigator is destroyed.")
            return StateTransition(this)
        }

        override fun stop(): StateTransition<Destination> {
            return StateTransition(this)
        }

        override fun destroy(): StateTransition<Destination> {
            return StateTransition(this)
        }

        override fun scene(scene: Scene<out Container>, data: TransitionData?) {
        }

        override fun childFinished(finishedDestination: Destination) {
        }

        override fun select(destination: Destination): StateTransition<Destination> {
            w("CompositeParallelNavigator.LifecycleState", "Warning: Cannot change destination after navigator is destroyed.")
            return StateTransition(this)
        }
    }

    private class StateTransition<D>(
        val newState: LifecycleState<D>,
        val action: (() -> Unit)? = null
    ) {

        fun andThen(f: (LifecycleState<D>) -> StateTransition<D>): StateTransition<D> {
            val newTransition = f(newState)
            return StateTransition(
                newTransition.newState
            ) {
                action?.invoke()
                newTransition.action?.invoke()
            }
        }
    }
}
