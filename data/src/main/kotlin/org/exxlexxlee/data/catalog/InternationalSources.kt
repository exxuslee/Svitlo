package org.exxlexxlee.data.catalog

import org.exxlexxlee.domain.model.Country.AUSTRALIA
import org.exxlexxlee.domain.model.Country.BRAZIL
import org.exxlexxlee.domain.model.Country.CANADA
import org.exxlexxlee.domain.model.Country.CHILE
import org.exxlexxlee.domain.model.Country.ECUADOR
import org.exxlexxlee.domain.model.Country.FINLAND
import org.exxlexxlee.domain.model.Country.FRANCE
import org.exxlexxlee.domain.model.Country.INDIA
import org.exxlexxlee.domain.model.Country.IRELAND
import org.exxlexxlee.domain.model.Country.ISRAEL
import org.exxlexxlee.domain.model.Country.KENYA
import org.exxlexxlee.domain.model.Country.MEXICO
import org.exxlexxlee.domain.model.Country.NEW_ZEALAND
import org.exxlexxlee.domain.model.Country.PAKISTAN
import org.exxlexxlee.domain.model.Country.PHILIPPINES
import org.exxlexxlee.domain.model.Country.POLAND
import org.exxlexxlee.domain.model.Country.SOUTH_AFRICA
import org.exxlexxlee.domain.model.Country.UKRAINE
import org.exxlexxlee.domain.model.Country.UNITED_KINGDOM
import org.exxlexxlee.domain.model.Country.UNITED_STATES
import org.exxlexxlee.domain.model.OutageSource
import org.exxlexxlee.domain.model.SourceAccess.ADDRESS
import org.exxlexxlee.domain.model.SourceAccess.ARCHIVED
import org.exxlexxlee.domain.model.SourceAccess.LIVE
import org.exxlexxlee.domain.model.SourceAccess.PERMISSION_REQUIRED
import org.exxlexxlee.domain.model.SourceAccess.UNAVAILABLE
import org.exxlexxlee.domain.model.SourceAccess.WEBSITE

object InternationalSources {
    val all = listOf(
        // Global / multi-country status + scheduled power (BlackoutMap aggregates utility feeds + probes)
        OutageSource(
            "blackoutmap",
            UKRAINE,
            "BlackoutMap · power & status · CC BY-NC 4.0",
            "https://blackoutmap.net/",
            "Europe/Kyiv",
            LIVE
        ),
        // Europe — ENTSO-E generation unavailability via Obsyd (no key, AGPL-3.0). Grid MW, not street schedules.
        OutageSource(
            "obsyd",
            POLAND,
            "Obsyd · ENTSO-E A77 forced outages · zone PL",
            "https://obsyd.dev/",
            "Europe/Warsaw",
            LIVE
        ),
        OutageSource(
            "obsyd",
            UNITED_KINGDOM,
            "Obsyd · ENTSO-E A77 forced outages · zone GB",
            "https://obsyd.dev/",
            "Europe/London",
            LIVE
        ),
        OutageSource(
            "obsyd",
            IRELAND,
            "Obsyd · ENTSO-E A77 forced outages · zone IE",
            "https://obsyd.dev/",
            "Europe/Dublin",
            LIVE
        ),
        // United States – free open-CORS live county/incident API
        OutageSource(
            "massoutage",
            UNITED_STATES,
            "MassOutage · live US counties & incidents · CC BY 4.0",
            "https://massoutage.com/",
            "America/New_York",
            LIVE
        ),
        OutageSource(
            "odin",
            UNITED_STATES,
            "ODIN · ORNL · County outage summaries",
            "https://odin.ornl.gov/",
            "America/New_York",
            LIVE
        ),
        // United Kingdom
        OutageSource(
            "ukpn",
            UNITED_KINGDOM,
            "UK Power Networks · CC BY 4.0",
            "https://ukpowernetworks.opendatasoft.com/explore/dataset/ukpn-live-faults/",
            "Europe/London",
            LIVE
        ),
        OutageSource(
            "ssen",
            UNITED_KINGDOM,
            "SSEN · Power Track",
            "https://powertrack.ssen.co.uk/powertrack",
            "Europe/London",
            LIVE
        ),
        OutageSource(
            "northern_powergrid",
            UNITED_KINGDOM,
            "Northern Powergrid · Live Power Cuts",
            "https://northernpowergrid.opendatasoft.com/explore/dataset/live-power-cuts-data/",
            "Europe/London",
            LIVE
        ),
        OutageSource(
            "nged",
            UNITED_KINGDOM,
            "National Grid Electricity Distribution",
            "https://powercuts.nationalgrid.co.uk/",
            "Europe/London",
            UNAVAILABLE
        ),
        // Canada
        OutageSource(
            "manitoba",
            CANADA,
            "Manitoba Hydro · Planned outages",
            "https://www.hydro.mb.ca/outages/outage-map/",
            "America/Winnipeg",
            LIVE
        ),
        OutageSource(
            "hydro_quebec",
            CANADA,
            "Hydro-Québec",
            "https://pannes.hydroquebec.com/poweroutages/",
            "America/Toronto",
            UNAVAILABLE
        ),
        // Poland
        OutageSource(
            "tauron",
            POLAND,
            "Tauron Dystrybucja",
            "https://www.tauron-dystrybucja.pl/wylaczenia",
            "Europe/Warsaw",
            ADDRESS
        ),
        OutageSource(
            "enea",
            POLAND,
            "Enea Operator",
            "https://wylaczenia.operator.enea.pl/",
            "Europe/Warsaw",
            WEBSITE
        ),
        OutageSource(
            "pge",
            POLAND,
            "PGE Dystrybucja",
            "https://pgedystrybucja.pl/planowane-wylaczenia",
            "Europe/Warsaw",
            WEBSITE
        ),
        OutageSource(
            "energa",
            POLAND,
            "Energa Operator",
            "https://energa-operator.pl/wylaczenia/wylaczenia-biezace",
            "Europe/Warsaw",
            WEBSITE
        ),
        // Only verified current feeds are LIVE. See apiplan.md for probe results and outstanding access requirements.
        OutageSource(
            "enedis",
            FRANCE,
            "Enedis · Open Data",
            "https://data.enedis.fr/",
            "Europe/Paris",
            WEBSITE
        ),
        OutageSource(
            "fingrid",
            FINLAND,
            "Fingrid · Open Data",
            "https://data.fingrid.fi/en/instructions",
            "Europe/Helsinki",
            PERMISSION_REQUIRED
        ),
        OutageSource(
            "ausgrid",
            AUSTRALIA,
            "Ausgrid",
            "https://www.ausgrid.com.au/Outages",
            "Australia/Sydney",
            WEBSITE
        ),
        OutageSource(
            "energex",
            AUSTRALIA,
            "Energex",
            "https://www.energex.com.au/outages",
            "Australia/Brisbane",
            WEBSITE
        ),
        OutageSource(
            "western_power",
            AUSTRALIA,
            "Western Power",
            "https://www.westernpower.com.au/outages/",
            "Australia/Perth",
            WEBSITE
        ),
        OutageSource(
            "vector",
            NEW_ZEALAND,
            "Vector",
            "https://www.vector.co.nz/personal/outages",
            "Pacific/Auckland",
            WEBSITE
        ),
        OutageSource(
            "powerco",
            NEW_ZEALAND,
            "Powerco",
            "https://www.powerco.co.nz/outages",
            "Pacific/Auckland",
            WEBSITE
        ),
        OutageSource(
            "npp",
            INDIA,
            "National Power Portal",
            "https://npp.gov.in/",
            "Asia/Kolkata",
            WEBSITE
        ),
        OutageSource(
            "urja_mitra",
            INDIA,
            "Urja Mitra",
            "https://urjamitra.com/",
            "Asia/Kolkata",
            WEBSITE
        ),
        OutageSource(
            "meralco",
            PHILIPPINES,
            "Meralco · Advisories",
            "https://company.meralco.com.ph/news-and-advisories",
            "Asia/Manila",
            WEBSITE
        ),
        OutageSource("ngcp", PHILIPPINES, "NGCP", "https://www.ngcp.ph/", "Asia/Manila", WEBSITE),
        OutageSource(
            "sec",
            CHILE,
            "SEC · Clientes sin suministro",
            "https://www.sec.cl/",
            "America/Santiago",
            WEBSITE
        ),
        // ANEEL's distribution interruptions dataset is historical reporting, not an upcoming household schedule.
        OutageSource(
            "aneel",
            BRAZIL,
            "ANEEL · Dados Abertos",
            "https://dadosabertos.aneel.gov.br/dataset/interrupcoes-de-energia-eletrica-nas-redes-de-distribuicao",
            "America/Sao_Paulo",
            WEBSITE
        ),
        OutageSource(
            "iec",
            ISRAEL,
            "Israel Electric Corporation",
            "https://www.iec.co.il/",
            "Asia/Jerusalem",
            WEBSITE
        ),
        // South Africa
        OutageSource(
            "eskom_calendar",
            SOUTH_AFRICA,
            "Eskom Calendar",
            "https://github.com/beyarkay/eskom-calendar",
            "Africa/Johannesburg",
            ARCHIVED
        ),
        OutageSource(
            "esp",
            SOUTH_AFRICA,
            "ESP",
            "https://esp.info/",
            "Africa/Johannesburg",
            PERMISSION_REQUIRED
        ),
        OutageSource(
            "cape_town",
            SOUTH_AFRICA,
            "City of Cape Town",
            "https://www.capetown.gov.za/Family%20and%20home/residential-utility-services/residential-electricity-services/load-shedding-and-outages",
            "Africa/Johannesburg",
            UNAVAILABLE
        ),
        // Other
        OutageSource(
            "pitc",
            PAKISTAN,
            "PITC · DISCO",
            "https://pitc.com.pk/",
            "Asia/Karachi",
            WEBSITE
        ),
        OutageSource("ke", PAKISTAN, "K-Electric", "https://ke.com.pk/", "Asia/Karachi", WEBSITE),
        OutageSource(
            "cnel",
            ECUADOR,
            "CNEL EP",
            "https://www.cnelep.gob.ec/",
            "America/Guayaquil",
            WEBSITE
        ),
        OutageSource(
            "esb",
            IRELAND,
            "ESB Networks · PowerCheck",
            "https://powercheck.esbnetworks.ie/",
            "Europe/Dublin",
            WEBSITE
        ),
        OutageSource(
            "kenya_power",
            KENYA,
            "Kenya Power",
            "https://www.kplc.co.ke/customer-support",
            "Africa/Nairobi",
            WEBSITE
        ),
        OutageSource("cfe", MEXICO, "CFE", "https://www.cfe.mx/", "America/Mexico_City", WEBSITE),
    )
}
