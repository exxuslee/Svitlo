package org.exxlexxlee.data.di

import java.time.Clock
import org.exxlexxlee.data.mapper.blackoutmap.BlackoutMapMapper
import org.exxlexxlee.data.mapper.manitoba.ManitobaMapper
import org.exxlexxlee.data.mapper.massoutage.MassOutageMapper
import org.exxlexxlee.data.mapper.obsyd.ObsydMapper
import org.exxlexxlee.data.mapper.tauron.TauronMapper
import org.exxlexxlee.data.mapper.ukpn.UkpnMapper
import org.exxlexxlee.data.remote.blackoutmap.BlackoutMapOutageFeed
import org.exxlexxlee.data.remote.international.InternationalOutageFeed
import org.exxlexxlee.data.remote.international.OutageAddressLookup
import org.exxlexxlee.data.remote.manitoba.ManitobaOutageFeed
import org.exxlexxlee.data.remote.massoutage.MassOutageFeed
import org.exxlexxlee.data.remote.northernpowergrid.NorthernPowergridOutageFeed
import org.exxlexxlee.data.remote.obsyd.ObsydOutageFeed
import org.exxlexxlee.data.remote.odin.OdinApi
import org.exxlexxlee.data.remote.odin.OdinOutageFeed
import org.exxlexxlee.data.remote.opendata.OpendatasoftApi
import org.exxlexxlee.data.remote.ssen.SsenApi
import org.exxlexxlee.data.remote.ssen.SsenOutageFeed
import org.exxlexxlee.data.remote.tauron.TauronApi
import org.exxlexxlee.data.remote.ukpn.UkpnOutageFeed
import org.exxlexxlee.data.repository.InternationalOutageRepositoryImpl
import org.exxlexxlee.domain.repository.InternationalOutageRepository
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

fun internationalOutageModule(clock: Clock = Clock.systemUTC()) = module {
    single { OpendatasoftApi(get(), get()) }
    single {
        BlackoutMapOutageFeed(
            get(),
            BlackoutMapMapper(get())
        )
    } bind InternationalOutageFeed::class
    single { MassOutageFeed(get(), MassOutageMapper(get())) } bind InternationalOutageFeed::class
    single { ObsydOutageFeed(get(), ObsydMapper(get()), clock) } bind InternationalOutageFeed::class
    single { UkpnOutageFeed(get(), UkpnMapper(get())) } bind InternationalOutageFeed::class
    single { NorthernPowergridOutageFeed(get()) } bind InternationalOutageFeed::class
    single { SsenOutageFeed(SsenApi(get(), get())) } bind InternationalOutageFeed::class
    single { OdinOutageFeed(OdinApi(get(), get())) } bind InternationalOutageFeed::class
    single { ManitobaOutageFeed(get(), ManitobaMapper(get())) } bind InternationalOutageFeed::class
    single { TauronApi(get(), TauronMapper(get()), clock) } binds arrayOf(
        InternationalOutageFeed::class, OutageAddressLookup::class,
    )
    single<InternationalOutageRepository> {
        InternationalOutageRepositoryImpl(getAll(), get(), clock, get())
    }
}
