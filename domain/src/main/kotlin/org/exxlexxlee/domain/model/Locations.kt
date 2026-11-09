package org.exxlexxlee.domain.model

data class Region(
    val id: String,
    val name: String,
    val providerId: String,
)

data class Supplier(
    val region: Region,
    val primary: Boolean,
)

data class ServiceArea(
    val id: String,
    val name: String,
    val suppliers: List<Supplier>,
)

data class City(
    val id: String,
    val name: String,
)

data class Street(
    val id: String,
    val name: String,
)

data class House(
    val id: String,
    val name: String,
    val number: String,
    val groupId: String,
)

data class Address(
    val region: Region,
    val city: City,
    val street: Street,
    val house: House,
)

