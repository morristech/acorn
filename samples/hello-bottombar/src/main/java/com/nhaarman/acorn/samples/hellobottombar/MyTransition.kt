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

import android.view.ViewGroup
import androidx.transition.TransitionManager
import com.nhaarman.acorn.android.transition.SceneTransition
import com.nhaarman.acorn.android.util.inflateView
import com.nhaarman.acorn.samples.hellobottombar.MyDestination.Favorites
import com.nhaarman.acorn.samples.hellobottombar.MyDestination.Music
import com.nhaarman.acorn.samples.hellobottombar.MyDestination.News
import com.nhaarman.acorn.samples.hellobottombar.MyDestination.Places

class MyTransition(
    private val newDestination: MyDestination
) : SceneTransition {

    override fun execute(parent: ViewGroup, callback: SceneTransition.Callback) {
        val contentContainer = parent.findViewById<ViewGroup>(R.id.contentContainer)

        TransitionManager.beginDelayedTransition(contentContainer)

        contentContainer.removeAllViews()
        contentContainer.inflateView(
            when (newDestination) {
                Favorites -> R.layout.favorites_scene
                Music -> R.layout.music_scene
                Places -> R.layout.places_scene
                News -> R.layout.news_scene
            },
            attachToParent = true
        )

        callback.onComplete(MyViewController(newDestination, parent))
    }
}