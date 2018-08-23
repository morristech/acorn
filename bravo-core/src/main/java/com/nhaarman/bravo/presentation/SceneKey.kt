package com.nhaarman.bravo.presentation

import kotlin.reflect.KClass

/**
 * A class representing the key for a Scene.
 */
class SceneKey(val value: String) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SceneKey

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "SceneKey(value='$value')"
    }

    companion object {

        /**
         * Create a [SceneKey] for given [Scene] class, consisting of its fully
         * qualified name.
         */
        fun <T : Scene<*>> from(sceneClass: KClass<T>): SceneKey {
            return SceneKey.from(sceneClass.java)
        }

        /**
         * Create a [SceneKey] for given [Scene] class, consisting of its fully
         * qualified name.
         */
        fun <T : Scene<*>> from(sceneClass: Class<T>): SceneKey {
            return SceneKey(sceneClass.name)
        }

        /**
         * Returns the default [SceneKey] for [T], consisting of its fully
         * qualified class name.
         */
        inline fun <reified T : Scene<*>> T.defaultKey(): SceneKey {
            return SceneKey.from(T::class)
        }

        inline fun <reified T : Scene<*>> defaultKey(): SceneKey {
            return from(T::class)
        }
    }
}