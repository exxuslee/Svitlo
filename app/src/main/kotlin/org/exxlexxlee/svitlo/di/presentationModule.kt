package org.exxlexxlee.svitlo.di

import org.exxlexxlee.domain.usecase.GetAreas
import org.exxlexxlee.svitlo.AppViewModel
import org.exxlexxlee.svitlo.features.address.AddressViewModel
import org.exxlexxlee.svitlo.features.home.HomeViewModel
import org.exxlexxlee.svitlo.features.international.InternationalViewModel
import org.exxlexxlee.svitlo.features.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { InternationalViewModel(get(), get(), get()) }
    viewModel { AppViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel {
        AddressViewModel(
            get<GetAreas>(),
            get(),
            get(),
            get(),
            get(),
            org.exxlexxlee.domain.model.Country.forLocale(android.content.res.Resources.getSystem().configuration.locales[0])
        )
    }
    viewModel { SettingsViewModel(get(), get(), get(), get(appVersion)) }
}
