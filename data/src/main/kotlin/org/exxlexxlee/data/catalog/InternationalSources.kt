package org.exxlexxlee.data.catalog

import org.exxlexxlee.domain.model.Country.*
import org.exxlexxlee.domain.model.OutageSource
import org.exxlexxlee.domain.model.SourceAccess.*

object InternationalSources {
    val all = listOf(
        OutageSource("ukpn", UNITED_KINGDOM, "UK Power Networks · CC BY 4.0", "https://ukpowernetworks.opendatasoft.com/explore/dataset/ukpn-live-faults/", "Europe/London", LIVE),
        OutageSource("nged", UNITED_KINGDOM, "National Grid Electricity Distribution", "https://powercuts.nationalgrid.co.uk/", "Europe/London", UNAVAILABLE),
        OutageSource("manitoba", CANADA, "Manitoba Hydro · Planned outages", "https://www.hydro.mb.ca/outages/outage-map/", "America/Winnipeg", LIVE),
        OutageSource("hydro_quebec", CANADA, "Hydro-Québec", "https://pannes.hydroquebec.com/poweroutages/", "America/Toronto", UNAVAILABLE),
        OutageSource("tauron", POLAND, "Tauron Dystrybucja", "https://www.tauron-dystrybucja.pl/wylaczenia", "Europe/Warsaw", ADDRESS),
        OutageSource("enea", POLAND, "Enea Operator", "https://wylaczenia.operator.enea.pl/", "Europe/Warsaw", WEBSITE),
        OutageSource("pge", POLAND, "PGE Dystrybucja", "https://pgedystrybucja.pl/planowane-wylaczenia", "Europe/Warsaw", WEBSITE),
        OutageSource("eskom_calendar", SOUTH_AFRICA, "Eskom Calendar", "https://github.com/beyarkay/eskom-calendar", "Africa/Johannesburg", ARCHIVED),
        OutageSource("esp", SOUTH_AFRICA, "ESP", "https://esp.info/", "Africa/Johannesburg", PERMISSION_REQUIRED),
        OutageSource("cape_town", SOUTH_AFRICA, "City of Cape Town", "https://www.capetown.gov.za/Family%20and%20home/residential-utility-services/residential-electricity-services/load-shedding-and-outages", "Africa/Johannesburg", UNAVAILABLE),
        OutageSource("pitc", PAKISTAN, "PITC · DISCO", "https://pitc.com.pk/", "Asia/Karachi", WEBSITE),
        OutageSource("ke", PAKISTAN, "K-Electric", "https://ke.com.pk/", "Asia/Karachi", WEBSITE),
        OutageSource("cnel", ECUADOR, "CNEL EP", "https://www.cnelep.gob.ec/", "America/Guayaquil", WEBSITE),
        OutageSource("esb", IRELAND, "ESB Networks · PowerCheck", "https://powercheck.esbnetworks.ie/", "Europe/Dublin", WEBSITE),
        OutageSource("kenya_power", KENYA, "Kenya Power", "https://www.kplc.co.ke/customer-support", "Africa/Nairobi", WEBSITE),
        OutageSource("cfe", MEXICO, "CFE", "https://www.cfe.mx/", "America/Mexico_City", WEBSITE),
    )
}
