package org.exxlexxlee.svitlo.di

import kotlinx.datetime.Clock
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
import org.exxlexxlee.svitlo.features.address.AddressViewModel
import org.exxlexxlee.svitlo.features.home.HomeViewModel
import org.exxlexxlee.svitlo.features.main.MainViewModel
import org.exxlexxlee.svitlo.features.settings.SettingsViewModel
import org.exxlexxlee.svitlo.widget.WidgetManager
import org.exxlexxlee.svitlo.widget.WidgetManagerImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
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
    single<Clock> { Clock.System }
    single<WidgetManager> { WidgetManagerImpl(androidContext()) }

    viewModel { MainViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get()) }
    viewModel { AddressViewModel(get<GetAreas>(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), androidContext()) }
}
