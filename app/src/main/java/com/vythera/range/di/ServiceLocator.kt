package com.vythera.range.di

import android.content.Context
import com.vythera.range.data.RangeStore

/**
 * Deliberately tiny DI: one repository, one store, no annotation processors and
 * therefore no build-time surprises.
 */
object ServiceLocator {
    lateinit var appContext: Context
        private set

    val store: RangeStore by lazy { RangeStore(appContext) }

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
