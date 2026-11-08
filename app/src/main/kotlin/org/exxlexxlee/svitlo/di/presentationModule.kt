package org.exxlexxlee.svitlo.di

import kotlinx.datetime.Clock
import org.exxlexxlee.domain.usecase.DeletePlace
import org.exxlexxlee.domain.usecase.GetAreas
import org.exxlexxlee.domain.usecase.GetGroups
import org.exxlexxlee.domain.usecase.GetRegions
import org.exxlexxlee.domain.usecase.ObserveDials
import org.exxlexxlee.domain.usecase.ObservePlace
import org.exxlexxlee.domain.usecase.ObservePlaces
import org.exxlexxlee.domain.usecase.ObserveSchedule
import org.exxlexxlee.domain.usecase.ObserveSelection
import org.exxlexxlee.domain.usecase.RefreshSchedule
import org.exxlexxlee.domain.usecase.SaveSelection
import org.exxlexxlee.svitlo.features.address.AddressViewModel
import org.exxlexxlee.svitlo.features.home.HomeViewModel
import org.exxlexxlee.svitlo.features.main.MainViewModel
import org.exxlexxlee.svitlo.features.schedule.ScheduleViewModel
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
    factory { RefreshSchedule(get()) }
    factory { ObserveDials(get()) }
    single<Clock> { Clock.System }

    viewModel { MainViewModel() }
    viewModel { HomeViewModel(get(), get(), get(), get(), get()) }
    viewModel { AddressViewModel(get<GetAreas>(), get(), get(), get(), get()) }
    viewModel { ScheduleViewModel(get(), get(), get(), get(), get(), get()) }
}
