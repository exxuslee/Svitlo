package org.exxlexxlee.svitlo.di

import org.exxlexxlee.domain.usecase.DeletePlace
import org.exxlexxlee.domain.usecase.GetAreas
import org.exxlexxlee.domain.usecase.GetDarkTheme
import org.exxlexxlee.domain.usecase.GetGroups
import org.exxlexxlee.domain.usecase.GetRegions
import org.exxlexxlee.domain.usecase.ObserveDarkTheme
import org.exxlexxlee.domain.usecase.ObservePlace
import org.exxlexxlee.domain.usecase.ObservePlaceSchedules
import org.exxlexxlee.domain.usecase.ObservePlaces
import org.exxlexxlee.domain.usecase.ObserveSchedule
import org.exxlexxlee.domain.usecase.ObserveSelection
import org.exxlexxlee.domain.usecase.RefreshSchedules
import org.exxlexxlee.domain.usecase.SaveSelection
import org.exxlexxlee.domain.usecase.SetDarkTheme
import org.koin.dsl.module

val useCaseModule = module {
    factory { GetRegions(get()) }
    factory { GetAreas(get()) }
    factory { GetGroups(get()) }
    factory { SaveSelection(get()) }
    factory { ObserveSelection(get()) }
    factory { ObservePlaces(get()) }
    factory { ObservePlace(get()) }
    factory { DeletePlace(get()) }
    factory { ObserveSchedule(get()) }
    factory { ObservePlaceSchedules(get(), get()) }
    factory { RefreshSchedules(get()) }
    factory { ObserveDarkTheme(get()) }
    factory { GetDarkTheme(get()) }
    factory { SetDarkTheme(get()) }
}
