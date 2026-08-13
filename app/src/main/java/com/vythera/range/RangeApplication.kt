package com.vythera.range

import android.app.Application
import com.vythera.range.di.ServiceLocator

class RangeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
