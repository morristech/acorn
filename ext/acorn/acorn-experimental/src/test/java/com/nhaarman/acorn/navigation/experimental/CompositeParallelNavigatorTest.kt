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

import com.nhaarman.acorn.navigation.Navigator
import com.nhaarman.acorn.navigation.SavableNavigator
import com.nhaarman.acorn.navigation.SingleSceneNavigator
import com.nhaarman.acorn.navigation.StackNavigator
import com.nhaarman.acorn.navigation.TransitionData
import com.nhaarman.acorn.navigation.experimental.CompositeParallelNavigatorTest.TestDestination.Destination1
import com.nhaarman.acorn.navigation.experimental.CompositeParallelNavigatorTest.TestDestination.Destination2
import com.nhaarman.acorn.presentation.Container
import com.nhaarman.acorn.presentation.Scene
import com.nhaarman.acorn.state.NavigatorState
import com.nhaarman.acorn.state.SavedState
import com.nhaarman.acorn.state.SceneState
import com.nhaarman.acorn.state.navigatorState
import com.nhaarman.expect.expect
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

@ExperimentalCompositeParallelNavigator
internal class CompositeParallelNavigatorTest {

    private val navigator1Scene1 = spy(SavableTestScene(11))
    private val navigator1Scene2 = spy(SavableTestScene(12))
    private val navigator2Scene1 = spy(SavableTestScene(21))
    private val navigator2Scene2 = spy(SavableTestScene(22))

    private val navigator1 = spy(SavableTestStackNavigator(listOf(navigator1Scene1)))
    private var navigator2 = spy(SavableTestStackNavigator(listOf(navigator2Scene1)))

    private val navigator = TestCompositeParallelNavigator()
    private val listener = mock<Navigator.Events>()

    @Nested
    inner class NavigatorStates {

        @Nested
        inner class InactiveNavigator {

            @Test
            fun `navigator is not finished`() {
                /* When */
                navigator.addNavigatorEventsListener(listener)

                /* Then */
                verify(listener, never()).finished()
            }

            @Test
            fun `navigator is not destroyed`() {
                /* Then */
                expect(navigator.isDestroyed()).toBe(false)
            }

            @Test
            fun `added listener does not get notified of scene`() {
                /* When */
                navigator.addNavigatorEventsListener(listener)

                /* Then */
                verify(listener, never()).scene(any(), any())
            }

            @Test
            fun `selecting a destination does not notify listeners of scene`() {
                /* Given */
                navigator.addNavigatorEventsListener(listener)

                /* When */
                navigator.select(Destination2)

                /* Then */
                verify(listener, never()).scene(any(), any())
            }
        }

        @Nested
        inner class ActiveNavigator {

            @Test
            fun `navigator is not finished`() {
                /* Given */
                navigator.onStart()

                /* When */
                navigator.addNavigatorEventsListener(listener)

                /* Then */
                verify(listener, never()).finished()
            }

            @Test
            fun `navigator is not destroyed`() {
                /* Given */
                navigator.onStart()

                /* Then */
                expect(navigator.isDestroyed()).toBe(false)
            }

            @Test
            fun `added listener gets notified of initial scene`() {
                /* Given */
                navigator.onStart()

                /* When */
                navigator.addNavigatorEventsListener(listener)

                /* Then */
                verify(listener).scene(navigator1Scene1, null)
            }

            @Test
            fun `removed listener does not get notified of scene`() {
                /* Given */
                val disposable = navigator.addNavigatorEventsListener(listener)
                disposable.dispose()

                /* When */
                navigator.onStart()

                /* Then */
                verify(listener, never()).scene(any(), anyOrNull())
            }

            @Test
            fun `non disposed listener is not disposed`() {
                /* Given */
                val disposable = navigator.addNavigatorEventsListener(listener)

                /* Then */
                expect(disposable.isDisposed()).toBe(false)
            }

            @Test
            fun `disposed listener is disposed`() {
                /* Given */
                val disposable = navigator.addNavigatorEventsListener(listener)

                /* When */
                disposable.dispose()

                /* Then */
                expect(disposable.isDisposed()).toBe(true)
            }

            @Test
            fun `starting navigator notifies listeners of initial scene`() {
                /* Given */
                navigator.addNavigatorEventsListener(listener)

                /* When */
                navigator.onStart()

                /* Then */
                verify(listener).scene(navigator1Scene1, null)
            }

            @Test
            fun `starting navigator multiple times notifies listeners of scene only once`() {
                /* Given */
                navigator.addNavigatorEventsListener(listener)

                /* When */
                navigator.onStart()
                navigator.onStart()

                /* Then */
                verify(listener, times(1)).scene(any(), anyOrNull())
            }

            @Test
            fun `starting navigator second time in a callback only notifies listener once`() {
                /* Given */
                navigator.addNavigatorEventsListener(listener)

                /* When */
                navigator.addNavigatorEventsListener(object : Navigator.Events {
                    override fun scene(scene: Scene<out Container>, data: TransitionData?) {
                        navigator.onStart()
                    }

                    override fun finished() {
                    }
                })
                navigator.onStart()

                /* Then */
                verify(listener, times(1)).scene(any(), anyOrNull())
            }

            @Test
            fun `starting navigator does not finish`() {
                /* Given */
                navigator.addNavigatorEventsListener(listener)

                /* When */
                navigator.onStart()

                /* Then */
                verify(listener, never()).finished()
            }

            @Test
            fun `changing destination notifies listeners of scene`() {
                /* Given */
                navigator.addNavigatorEventsListener(listener)
                navigator.onStart()

                /* When */
                navigator.select(Destination2)

                /* Then */
                verify(listener).scene(eq(navigator2Scene1), anyOrNull())
            }

            @Test
            fun `changing to the same destination twice notifies listeners of scene only once`() {
                /* Given */
                navigator.addNavigatorEventsListener(listener)
                navigator.onStart()

                /* When */
                navigator.select(Destination2)
                navigator.select(Destination2)

                /* Then */
                verify(listener, times(1)).scene(eq(navigator2Scene1), anyOrNull())
            }

            @Test
            fun `start navigator after destination changed notifies pushed scene`() {
                /* Given */
                navigator.addNavigatorEventsListener(listener)
                navigator.select(Destination2)

                /* When */
                navigator.onStart()

                /* Then */
                verify(listener).scene(navigator2Scene1)
            }
        }

        @Nested
        inner class StoppedNavigator {

            @Test
            fun `stopping navigator does not finish`() {
                /* Given */
                navigator.addNavigatorEventsListener(listener)

                /* When */
                navigator.onStop()

                /* Then */
                verify(listener, never()).finished()
            }

            @Test
            fun `stopped navigator is not destroyed`() {
                /* When */
                navigator.onStop()

                /* Then */
                expect(navigator.isDestroyed()).toBe(false)
            }
        }

        @Nested
        inner class DestroyedNavigator {

            @Test
            fun `destroying navigator does not finish`() {
                /* Given */
                navigator.addNavigatorEventsListener(listener)

                /* When */
                navigator.onDestroy()

                /* Then */
                verify(listener, never()).finished()
            }

            @Test
            fun `isDestroyed returns true`() {
                /* When */
                navigator.onDestroy()

                /* Then */
                expect(navigator.isDestroyed()).toBe(true)
            }

            @Test
            fun `changing destination for destroyed navigator does not notify listeners`() {
                /* Given */
                navigator.addNavigatorEventsListener(listener)
                navigator.onDestroy()

                /* When */
                navigator.select(Destination2)

                /* Then */
                verify(listener, never()).scene(any(), any())
            }
        }
    }

    @Nested
    inner class SingleChildNavigatorInteraction {

        @Test
        fun `starting navigator starts initial navigator`() {
            /* When */
            navigator.onStart()

            /* Then */
            navigator1.inOrder {
                verify().onStart()
                verifyNoMoreInteractions()
            }
        }

        @Test
        fun `starting navigator multiple times starts initial navigator only once`() {
            /* When */
            navigator.onStart()
            navigator.onStart()

            /* Then */
            verify(navigator1, times(1)).onStart()
        }

        @Test
        fun `stopping an inactive navigator does not stop child navigator`() {
            /* When */
            navigator.onStop()

            /* Then */
            verify(navigator1, never()).onStop()
        }

        @Test
        fun `stopping an active navigator stops child navigator`() {
            /* Given */
            navigator.onStart()

            /* When */
            navigator.onStop()

            /* Then */
            navigator1.inOrder {
                verify().onStart()
                verify().onStop()
            }
        }

        @Test
        fun `destroy an inactive navigator does not stop child navigator`() {
            /* When */
            navigator.onDestroy()

            /* Then */
            verify(navigator1, never()).onStop()
        }

        @Test
        fun `destroy an inactive navigator does destroy child navigator`() {
            /* When */
            navigator.onDestroy()

            /* Then */
            verify(navigator1).onDestroy()
        }

        @Test
        fun `destroy an active navigator stops and destroys child navigator`() {
            /* Given */
            navigator.onStart()

            /* When */
            navigator.onDestroy()

            /* Then */
            navigator1.inOrder {
                verify().onStart()
                verify().onStop()
                verify().onDestroy()
            }
        }

        @Test
        fun `starting a destroyed navigator does not start child navigator`() {
            /* Given */
            navigator.onDestroy()

            /* When */
            navigator.onStart()

            /* Then */
            verify(navigator1).onDestroy()
            verify(navigator1, never()).onStart()
        }

        @Test
        fun `stopping a destroyed navigator does not start child navigator`() {
            /* Given */
            navigator.onDestroy()

            /* When */
            navigator.onStop()

            /* Then */
            verify(navigator1).onDestroy()
            verify(navigator1, never()).onStop()
        }

        @Test
        fun `destroying a destroyed navigator only destroys child navigator once`() {
            /* Given */
            navigator.onDestroy()

            /* When */
            navigator.onDestroy()

            /* Then */
            verify(navigator1, times(1)).onDestroy()
        }

        @Test
        fun `starting an inactive navigator results in scene emission`() {
            /* Given */
            navigator.addNavigatorEventsListener(listener)

            /* When */
            navigator.onStart()

            /* Then */
            verify(listener).scene(eq(navigator1Scene1), anyOrNull())
            verifyNoMoreInteractions(listener)
        }

        @Test
        fun `an inactive navigator does not propagate child navigator emitting scene`() {
            /* Given */
            navigator.addNavigatorEventsListener(listener)

            /* When */
            navigator1.push(navigator1Scene2)

            /* Then */
            verify(listener, never()).scene(any(), anyOrNull())
        }

        @Test
        fun `an active navigator propagates child navigator emitting scene`() {
            /* Given */
            navigator.addNavigatorEventsListener(listener)
            navigator.onStart()

            /* When */
            navigator1.push(navigator1Scene2)

            /* Then */
            listener.inOrder {
                verify().scene(eq(navigator1Scene1), anyOrNull())
                verify().scene(eq(navigator1Scene2), anyOrNull())
            }
        }

        @Test
        fun `a destroyed navigator does not propagate child navigator emitting scene`() {
            /* Given */
            navigator.addNavigatorEventsListener(listener)
            navigator.onStart()
            navigator.onStop()
            navigator.onDestroy()

            /* When */
            navigator1.push(navigator1Scene2)

            /* Then */
            verify(listener, never()).scene(eq(navigator1Scene2), anyOrNull())
        }

        @Test
        fun `a stopped navigator propagates child navigator emitting scene when started`() {
            /* Given */
            navigator.addNavigatorEventsListener(listener)
            navigator.onStart()
            navigator.onStop()

            /* When */
            navigator1.push(navigator1Scene2)
            navigator.onStart()

            /* Then */
            verify(listener).scene(eq(navigator1Scene2), anyOrNull())
        }

        @Test
        fun `a stopped navigator propagates child navigator emitting scene when started for late listeners`() {
            /* Given */
            navigator.onStart()
            navigator.addNavigatorEventsListener(listener)
            navigator.onStop()

            /* When */
            navigator1.push(navigator1Scene2)
            navigator.onStart()

            /* Then */
            verify(listener).scene(eq(navigator1Scene2), anyOrNull())
        }

        @Test
        fun `a removed listener during active state does not get notified of scenes anymore when stopped and started`() {
            /* Given */
            val disposable = navigator.addNavigatorEventsListener(listener)
            navigator.onStart()
            disposable.dispose()
            navigator.onStop()

            /* When */
            navigator1.push(navigator1Scene2)
            navigator.onStart()

            /* Then */
            verify(listener, never()).scene(eq(navigator1Scene2), anyOrNull())
        }
    }

    @Nested
    inner class MultipleChildrenNavigatorInteraction {

        @Test
        fun `changing destination for inactive navigator does not start selected child navigator`() {
            /* When */
            navigator.select(Destination2)

            /* Then */
            verify(navigator2, never()).onStart()
        }

        @Test
        fun `starting navigator after destination change starts selected child navigator`() {
            /* Given */
            navigator.select(Destination2)

            /* When */
            navigator.onStart()

            /* Then */
            verify(navigator2).onStart()
        }

        @Test
        fun `changing destination after navigator started starts selected child navigator`() {
            /* Given */
            navigator.onStart()

            /* When */
            navigator.select(Destination2)

            /* Then */
            verify(navigator2).onStart()
        }

        @Test
        fun `changing to the same destination twice after navigator started starts selected child navigator only once`() {
            /* Given */
            navigator.onStart()

            /* When */
            navigator.select(Destination2)
            navigator.select(Destination2)

            /* Then */
            verify(navigator2, times(1)).onStart()
        }

        @Test
        fun `starting navigator after destination change does not start initial child navigator`() {
            /* Given */
            navigator.select(Destination2)

            /* When */
            navigator.onStart()

            /* Then */
            verify(navigator1, never()).onStart()
        }

        @Test
        fun `changing destination after navigator started stops initial child navigator`() {
            /* Given */
            navigator.onStart()

            /* When */
            navigator.select(Destination2)

            /* Then */
            navigator1.inOrder {
                verify().onStart()
                verify().onStop()
                verifyNoMoreInteractions()
            }
        }

        @Test
        fun `stopping inactive navigator does not stop children`() {
            /* Given */
            navigator.select(Destination2)

            /* When */
            navigator.onStop()

            /* Then */
            verify(navigator1, never()).onStop()
            verify(navigator2, never()).onStop()
        }

        @Test
        fun `stopping active navigator stops only selected navigator`() {
            /* Given */
            navigator.select(Destination2)
            navigator.onStart()

            /* When */
            navigator.onStop()

            /* Then */
            verify(navigator1, never()).onStop()
            navigator2.inOrder {
                verify().onStart()
                verify().onStop()
            }
        }

        @Test
        fun `stopping destroyed navigator does not stop children`() {
            /* Given */
            navigator.select(Destination2)
            navigator.onDestroy()

            /* When */
            navigator.onStop()

            /* Then */
            verify(navigator1, never()).onStop()
            verify(navigator2, never()).onStop()
        }

        @Test
        fun `destroying inactive navigator destroys all children but does not stop them`() {
            /* Given */
            navigator.select(Destination2)

            /* When */
            navigator.onDestroy()

            /* Then */
            verify(navigator1, never()).onStop()
            verify(navigator1).onDestroy()
            verify(navigator2, never()).onStop()
            verify(navigator2).onDestroy()
        }

        @Test
        fun `destroying active navigator stops selected child and destroys all children`() {
            /* Given */
            navigator.select(Destination2)
            navigator.onStart()

            /* When */
            navigator.onDestroy()

            /* Then */
            verify(navigator1, never()).onStop()
            verify(navigator1).onDestroy()
            verify(navigator2).onStop()
            verify(navigator2).onDestroy()
        }

        @Test
        fun `destroying destroyed navigator does not destroy child navigators again`() {
            /* Given */
            navigator.select(Destination2)
            navigator.onDestroy()

            /* When */
            navigator.onDestroy()

            /* Then */
            verify(navigator1, times(1)).onDestroy()
            verify(navigator2, times(1)).onDestroy()
        }

        @Test
        fun `changing destination propagates scene for selected destination`() {
            /* Given */
            navigator.addNavigatorEventsListener(listener)
            navigator.onStart()

            /* When */
            navigator.select(Destination2)

            /* Then */
            listener.inOrder {
                verify().scene(eq(navigator1Scene1), anyOrNull())
                verify().scene(eq(navigator2Scene1), anyOrNull())
            }
        }

        @Test
        fun `changing destination multiple times propagates scene for selected destinations`() {
            /* Given */
            navigator.addNavigatorEventsListener(listener)
            navigator.onStart()

            /* When */
            navigator.select(Destination2)
            navigator.select(Destination1)

            /* Then */
            listener.inOrder {
                verify().scene(eq(navigator1Scene1), anyOrNull())
                verify().scene(eq(navigator2Scene1), anyOrNull())
                verify().scene(eq(navigator1Scene1), anyOrNull())
            }
        }

        @Test
        fun `an active navigator propagates selected child navigator emitting scene`() {
            /* Given */
            navigator.addNavigatorEventsListener(listener)
            navigator.select(Destination2)
            navigator.onStart()

            /* When */
            navigator2.push(navigator2Scene2)

            /* Then */
            verify(listener).scene(eq(navigator2Scene2), anyOrNull())
        }

        @Test
        fun `an active navigator does not propagate non-selected child navigator emitting scene`() {
            /* Given */
            navigator.addNavigatorEventsListener(listener)
            navigator.select(Destination2)
            navigator.onStart()

            /* When */
            navigator1.push(navigator1Scene2)

            /* Then */
            verify(listener).scene(eq(navigator2Scene1), anyOrNull())
            verify(listener, never()).scene(eq(navigator1Scene2), anyOrNull())
        }
    }

    @Nested
    inner class ChildrenFinishing {

        @Test
        fun `the primary destination finishing finishes inactive navigator`() {
            /* Given */
            navigator.addNavigatorEventsListener(listener)

            /* When */
            navigator1.finish()

            /* Then */
            verify(listener).finished()
        }

        @Test
        fun `a non-primary, non-selected destination finishing does not finish inactive navigator`() {
            /* Given */
            navigator.addNavigatorEventsListener(listener)
            navigator.select(Destination2)
            navigator.select(Destination1)

            /* When */
            navigator2.finish()

            /* Then */
            verify(listener, never()).finished()
        }

        @Test
        fun `a non-primary, selected destination finishing selects initial navigator in inactive state`() {
            /* Given */
            navigator.addNavigatorEventsListener(listener)
            navigator.select(Destination2)

            /* When */
            navigator2.finish()
            navigator.onStart()

            /* Then */
            listener.inOrder {
                verify().scene(eq(navigator1Scene1), anyOrNull())
                verifyNoMoreInteractions()
            }
        }

        @Test
        fun `selecting a finished navigator during inactive state recreates it`() {
            /* Given */
            navigator.addNavigatorEventsListener(listener)
            navigator.select(Destination2)
            navigator.select(Destination1)

            /* When */
            navigator2.finish()
            navigator2 = spy(SavableTestStackNavigator(listOf(navigator2Scene1)))
            navigator.select(Destination2)
            navigator.onStart()

            /* Then */
            listener.inOrder {
                verify().scene(eq(navigator2Scene1), anyOrNull())
                verifyNoMoreInteractions()
            }
        }

        @Test
        fun `the primary destination finishing finishes active navigator`() {
            /* Given */
            navigator.addNavigatorEventsListener(listener)
            navigator.onStart()

            /* When */
            navigator1.finish()

            /* Then */
            verify(listener).finished()
        }

        @Test
        fun `a non-primary, non-selected destination finishing does not finish active navigator`() {
            /* Given */
            navigator.addNavigatorEventsListener(listener)
            navigator.select(Destination2)
            navigator.select(Destination1)
            navigator.onStart()

            /* When */
            navigator2.finish()

            /* Then */
            verify(listener, never()).finished()
        }

        @Test
        fun `a non-primary, selected destination finishing selects initial navigator in active state`() {
            /* Given */
            navigator.addNavigatorEventsListener(listener)
            navigator.select(Destination2)
            navigator.onStart()

            /* When */
            navigator2.finish()

            /* Then */
            listener.inOrder {
                verify().scene(eq(navigator2Scene1), anyOrNull())
                verify().scene(eq(navigator1Scene1), anyOrNull())
            }
        }

        @Test
        fun `selecting a finished navigator during active state recreates it`() {
            /* Given */
            navigator.addNavigatorEventsListener(listener)
            navigator.select(Destination2)
            navigator.select(Destination1)
            navigator.onStart()

            /* When */
            navigator2.finish()
            navigator2 = spy(SavableTestStackNavigator(listOf(navigator2Scene1)))
            navigator.select(Destination2)

            /* Then */
            listener.inOrder {
                verify().scene(eq(navigator1Scene1), anyOrNull())
                verify().scene(eq(navigator2Scene1), anyOrNull())
                verifyNoMoreInteractions()
            }
        }
    }

    @Nested
    inner class SavingState {

        private val navigator1Scene1 = SavableTestScene(11)
        private val navigator2Scene1 = SavableTestScene(21)

        private val navigator1 = TestSingleSceneNavigator(navigator1Scene1)
        private val navigator2 = TestSingleSceneNavigator(navigator2Scene1)
        private val savableNavigator1 = SavableTestSingleSceneNavigator(navigator1Scene1)
        private val savableNavigator2 = SavableTestSingleSceneNavigator(navigator2Scene1)

        @Test
        fun `CompositeParallelNavigator does not implement SavableNavigator by default`() {
            /* Given */
            val navigator: Navigator = TestCompositeParallelNavigator()

            /* Then */
            expect(navigator is SavableNavigator).toBe(false)
        }

        @Test
        fun `saving and restoring state for single savable navigator`() {
            /* Given */
            val navigator = SavableTestCompositeParallelNavigator(
                mapOf(Destination1 to savableNavigator1),
                null
            )
            navigator.onStart()
            navigator1Scene1.foo = 3

            /* When */
            val bundle = navigator.saveInstanceState()
            navigator1Scene1.foo = 42

            val restoredNavigator = SavableTestCompositeParallelNavigator(
                mapOf(Destination1 to savableNavigator1),
                bundle
            )
            restoredNavigator.onStart()
            restoredNavigator.addNavigatorEventsListener(listener)

            /* Then */
            argumentCaptor<Scene<out Container>> {
                verify(listener).scene(capture(), anyOrNull())
                expect(lastValue).toNotBeTheSameAs(navigator1Scene1)
                expect(lastValue).toBeInstanceOf<SavableTestScene> {
                    expect(it.foo).toBe(3)
                }
            }
        }

        @Test
        fun `saving and restoring state for single non savable navigator`() {
            /* Given */
            val navigator = SavableTestCompositeParallelNavigator(
                mapOf(Destination1 to navigator1),
                null
            )
            navigator.onStart()
            navigator1Scene1.foo = 3

            /* When */
            val bundle = navigator.saveInstanceState()
            navigator1Scene1.foo = 42

            val restoredNavigator = SavableTestCompositeParallelNavigator(
                mapOf(Destination1 to navigator2),
                bundle
            )
            restoredNavigator.onStart()
            restoredNavigator.addNavigatorEventsListener(listener)

            /* Then */
            argumentCaptor<Scene<out Container>> {
                verify(listener).scene(capture(), anyOrNull())
                expect(lastValue).toBeInstanceOf<SavableTestScene> {
                    expect(it.foo).toBe(21)
                }
            }
        }

        @Test
        fun `saving and restoring state for multiple savable navigators`() {
            /* Given */
            val navigator = SavableTestCompositeParallelNavigator(
                mapOf(
                    Destination1 to savableNavigator1,
                    Destination2 to savableNavigator2
                ),
                null
            )
            navigator.onStart()
            navigator1Scene1.foo = 3
            navigator.select(Destination2)
            navigator2Scene1.foo = 4

            /* When */
            val bundle = navigator.saveInstanceState()
            navigator2Scene1.foo = 42

            val restoredNavigator = SavableTestCompositeParallelNavigator(
                mapOf(
                    Destination1 to savableNavigator1,
                    Destination2 to savableNavigator2
                ),
                bundle
            )
            restoredNavigator.onStart()
            restoredNavigator.addNavigatorEventsListener(listener)

            /* Then */
            argumentCaptor<Scene<out Container>> {
                verify(listener).scene(capture(), anyOrNull())
                expect(lastValue).toNotBeTheSameAs(navigator2Scene1)
                expect(lastValue).toBeInstanceOf<SavableTestScene> {
                    expect(it.foo).toBe(4)
                }
            }
        }

        @Test
        fun `restoring from malformed state falls back to initial state - empty state`() {
            /* Given */
            val bundle = NavigatorState()

            /* When */
            val restoredNavigator = SavableTestCompositeParallelNavigator(
                mapOf(
                    Destination1 to savableNavigator1,
                    Destination2 to savableNavigator2
                ),
                bundle
            )
            restoredNavigator.onStart()
            restoredNavigator.addNavigatorEventsListener(listener)

            /* Then */
            argumentCaptor<Scene<out Container>> {
                verify(listener).scene(capture(), anyOrNull())
                expect(lastValue).toBeTheSameAs(navigator1Scene1)
                expect(lastValue).toBeInstanceOf<SavableTestScene> {
                    expect(it.foo).toBe(11)
                }
            }
        }

        @Test
        fun `restoring from malformed state falls back to initial state - wrong size`() {
            /* Given */
            val bundle = navigatorState {
                it["size"] = 2
            }

            /* When */
            val restoredNavigator = SavableTestCompositeParallelNavigator(
                mapOf(
                    Destination1 to savableNavigator1,
                    Destination2 to savableNavigator2
                ),
                bundle
            )
            restoredNavigator.onStart()
            restoredNavigator.addNavigatorEventsListener(listener)

            /* Then */
            argumentCaptor<Scene<out Container>> {
                verify(listener).scene(capture(), anyOrNull())
                expect(lastValue).toBeTheSameAs(navigator1Scene1)
                expect(lastValue).toBeInstanceOf<SavableTestScene> {
                    expect(it.foo).toBe(11)
                }
            }
        }

        @Test
        fun `restoring from malformed state falls back to initial state - missing destination`() {
            /* Given */
            val bundle = navigatorState {
                it["size"] = 1
                it["0_state"] = NavigatorState()
                it["0_destination"] = "0"
            }

            /* When */
            val restoredNavigator = SavableTestCompositeParallelNavigator(
                mapOf(
                    Destination1 to savableNavigator1,
                    Destination2 to savableNavigator2
                ),
                bundle
            )
            restoredNavigator.onStart()
            restoredNavigator.addNavigatorEventsListener(listener)

            /* Then */
            argumentCaptor<Scene<out Container>> {
                verify(listener).scene(capture(), anyOrNull())
                expect(lastValue).toBeTheSameAs(navigator1Scene1)
                expect(lastValue).toBeInstanceOf<SavableTestScene> {
                    expect(it.foo).toBe(11)
                }
            }
        }

        @Test
        fun `saved state from callback is the same as saved state _after_ callback`() {
            /* Given */
            val navigator = SavableTestCompositeParallelNavigator(
                mapOf(
                    Destination1 to savableNavigator1,
                    Destination2 to savableNavigator2
                ),
                null
            )

            var state1: SavedState? = null
            navigator.addNavigatorEventsListener(object : Navigator.Events {
                override fun scene(scene: Scene<out Container>, data: TransitionData?) {
                    state1 = navigator.saveInstanceState()
                }

                override fun finished() {
                }
            })
            navigator.onStart()

            /* When */
            navigator.select(Destination2)
            val state2 = navigator.saveInstanceState()

            /* Then */
            expect(state1).toBe(state2)
        }

        @Test
        fun `saving a destroyed navigator results in empty state`() {
            /* Given */
            val navigator = SavableTestCompositeParallelNavigator(
                mapOf(Destination1 to savableNavigator1),
                null
            )
            navigator.onStart()
            navigator.onStop()
            navigator.onDestroy()

            /* When */
            val state = navigator.saveInstanceState()

            /* Then */
            expect(state.entries.size).toBe(0)
        }
    }

    private enum class TestDestination {
        Destination1,
        Destination2
    }

    private inner class TestCompositeParallelNavigator : CompositeParallelNavigator<TestDestination>(Destination1, null) {

        override fun serialize(destination: TestDestination): String {
            error("Not used")
        }

        override fun deserialize(serializedDestination: String): TestDestination {
            error("Not used")
        }

        override fun createNavigator(destination: TestDestination, savedState: NavigatorState?): Navigator {
            return when (destination) {
                Destination1 -> navigator1
                Destination2 -> navigator2
            }
        }
    }

    private inner class SavableTestCompositeParallelNavigator(
        private val navigators: Map<TestDestination, Navigator>,
        savedState: NavigatorState?
    ) : CompositeParallelNavigator<TestDestination>(Destination1, savedState), SavableNavigator {

        override fun serialize(destination: TestDestination): String {
            return "${destination.ordinal}"
        }

        override fun deserialize(serializedDestination: String): TestDestination {
            return TestDestination.values()[serializedDestination.toInt()]
        }

        override fun createNavigator(destination: TestDestination, savedState: NavigatorState?): Navigator {
            if (savedState != null) {
                return when (destination) {
                    Destination1 -> SavableTestSingleSceneNavigator(navigator1Scene1, savedState)
                    Destination2 -> SavableTestSingleSceneNavigator(navigator2Scene1, savedState)
                }
            }

            return navigators[destination]!!
        }
    }

    private open class SavableTestStackNavigator(
        private val initialStack: List<SavableTestScene>,
        savedState: NavigatorState? = null
    ) : StackNavigator(savedState), SavableNavigator {

        override fun initialStack(): List<Scene<out Container>> {
            return initialStack
        }

        override fun instantiateScene(sceneClass: KClass<out Scene<*>>, state: SceneState?): Scene<*> {
            return when (sceneClass) {
                SavableTestScene::class -> SavableTestScene.create(state)
                else -> error("Unknown class: $sceneClass")
            }
        }
    }

    open class TestSingleSceneNavigator(
        private val scene: Scene<out Container>
    ) : SingleSceneNavigator(null) {

        override fun createScene(state: SceneState?): Scene<out Container> {
            return scene
        }
    }

    open class SavableTestSingleSceneNavigator(
        private val scene: Scene<out Container>,
        savedState: NavigatorState? = null
    ) : SingleSceneNavigator(savedState), SavableNavigator {

        override fun createScene(state: SceneState?): Scene<out Container> {
            return state?.let { SavableTestScene.create(it) } ?: scene
        }
    }
}
