package org.exxlexxlee.svitlo.features.address

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Church
import androidx.compose.material.icons.outlined.CorporateFare
import androidx.compose.material.icons.outlined.Cottage
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.material.icons.outlined.Garage
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.House
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.ui.graphics.vector.ImageVector

enum class AddressIcon(val id: String, val image: ImageVector) {
    Home("home", Icons.Outlined.Home),
    Apartment("apartment", Icons.Outlined.Apartment),
    HomeWork("home_work", Icons.Outlined.HomeWork),
    House("house", Icons.Outlined.House),
    Cottage("cottage", Icons.Outlined.Cottage),
    Office("office", Icons.Outlined.Business),
    Bed("bed", Icons.Outlined.Bed),
    Garage("garage", Icons.Outlined.Garage),
    Store("store", Icons.Outlined.Storefront),
    Building("building", Icons.Outlined.CorporateFare),
    Factory("factory", Icons.Outlined.Factory),
    Warehouse("warehouse", Icons.Outlined.Warehouse),
    School("school", Icons.Outlined.School),
    Hospital("hospital", Icons.Outlined.LocalHospital),
    Church("church", Icons.Outlined.Church),
    ;

    companion object {
        fun fromId(id: String): AddressIcon = entries.find { it.id == id } ?: Home

        val picker: List<AddressIcon> = listOf(
            Home,
            Apartment,
            HomeWork,
            House,
            Cottage,
            Office,
            Bed,
            Garage,
            Store,
            Building,
            Factory,
            Warehouse,
            School,
            Hospital,
            Church,
        )
    }
}
