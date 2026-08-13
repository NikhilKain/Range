package com.vythera.range.di

import android.content.Context

object ServiceLocator {
    lateinit var appContext: Context
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
