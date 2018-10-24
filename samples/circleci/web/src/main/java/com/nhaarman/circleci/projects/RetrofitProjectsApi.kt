/*
 * Acorn - Decoupling navigation from Android
 * Copyright (C) 2018 Niek Haarman
 *
 * Acorn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Acorn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Acorn.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.nhaarman.circleci.projects

import com.nhaarman.circleci.Project
import com.nhaarman.circleci.builds.BuildAdapter
import com.nhaarman.httpmonads.HttpTry
import com.squareup.moshi.Moshi
import io.reactivex.Single
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

internal interface RetrofitProjectsApi {

    @GET("projects")
    fun retrieveProjects(): Single<HttpTry<List<Project>>>

    companion object {

        fun create(baseRetrofit: Retrofit): RetrofitProjectsApi {
            return baseRetrofit
                .newBuilder()
                .addConverterFactory(
                    MoshiConverterFactory.create(
                        Moshi.Builder()
                            .add(BuildAdapter)
                            .add(ProjectAdapter)
                            .build()
                    )
                )
                .build()
                .create(RetrofitProjectsApi::class.java)
        }
    }
}