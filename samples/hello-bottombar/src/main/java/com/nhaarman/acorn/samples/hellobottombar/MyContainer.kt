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

import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.nhaarman.acorn.android.presentation.ViewController
import com.nhaarman.acorn.presentation.Container
import com.nhaarman.acorn.samples.hellobottombar.MyDestination.Favorites
import com.nhaarman.acorn.samples.hellobottombar.MyDestination.Music
import com.nhaarman.acorn.samples.hellobottombar.MyDestination.News
import com.nhaarman.acorn.samples.hellobottombar.MyDestination.Places

interface MyContainer : Container {

    fun setDestinationSelectedListener(listener: DestinationSelectedListener)
}

class MyViewController(
    destination: MyDestination,
    override val view: View
) : ViewController, MyContainer {

    init {
        view
            .findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            .selectedItemId = when (destination) {
            Favorites -> R.id.favorites
            Music -> R.id.music
            Places -> R.id.places
            News -> R.id.news
        }
    }

    override fun setDestinationSelectedListener(listener: DestinationSelectedListener) {
        view
            .findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            .setOnNavigationItemSelectedListener { item ->
                listener.onDestinationSelected(
                    when (item.itemId) {
                        R.id.favorites -> Favorites
                        R.id.music -> Music
                        R.id.places -> Places
                        R.id.news -> News
                        else -> error("Invalid item: $item")
                    }
                )
                true
            }
    }
}
