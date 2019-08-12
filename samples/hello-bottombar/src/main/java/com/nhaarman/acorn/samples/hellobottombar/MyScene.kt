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

package com.nhaarman.acorn.samples.hellobottombar

import com.nhaarman.acorn.presentation.BasicScene
import com.nhaarman.acorn.presentation.SavableScene
import com.nhaarman.acorn.state.SceneState
import com.nhaarman.acorn.state.get

class MyScene(
    val destination: MyDestination,
    private val value: Int,
    private val listener: Events,
    savedState: SceneState? = null
) : BasicScene<MyContainer>(savedState),
    SavableScene {

    override fun attach(v: MyContainer) {
        super.attach(v)
        v.setDestinationSelectedListener(listener)
    }

    override fun saveInstanceState(): SceneState {
        return super.saveInstanceState()
            .also {
                it["value"] = value
            }
    }

    interface Events : DestinationSelectedListener

    companion object {

        fun from(
            destination: MyDestination,
            listener: Events,
            savedState: SceneState
        ): MyScene {
            return MyScene(
                destination,
                savedState["value"]!!,
                listener,
                savedState
            )
        }
    }
}