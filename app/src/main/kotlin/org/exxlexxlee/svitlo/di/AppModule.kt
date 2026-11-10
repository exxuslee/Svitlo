package org.exxlexxlee.svitlo.di

import org.exxlexxlee.data.di.dataModule
import org.koin.dsl.module

val appModule = module {
    includes(dataModule, useCaseModule, platformModule, presentationModule, firebaseModule)
}
