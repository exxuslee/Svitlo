package org.exxlexxlee.svitlo.features.international.models

import org.exxlexxlee.domain.model.AddressDetails
import org.exxlexxlee.domain.model.AddressOption
import org.exxlexxlee.domain.model.Country
import org.exxlexxlee.domain.model.InternationalOutage
import org.exxlexxlee.domain.model.OutageSource
import org.exxlexxlee.domain.model.SourceAccess
import java.time.Instant

private fun SourceAccess?.steps(): List<InternationalStep> =
    listOf(InternationalStep.Supplier) + when (this) {
        SourceAccess.LIVE -> listOf(InternationalStep.Location)
        SourceAccess.ADDRESS -> listOf(
            InternationalStep.City,
            InternationalStep.Street,
            InternationalStep.House
        )

        else -> emptyList()
    } + InternationalStep.Details

data class ViewState(
    val country: Country,
    val now: Instant = Instant.now(),
    val sources: List<OutageSource> = emptyList(),
    val source: OutageSource? = null,
    val step: InternationalStep = InternationalStep.Supplier,
    val outages: List<InternationalOutage> = emptyList(),
    val query: String = "",
    val cityQuery: String = "",
    val streetQuery: String = "",
    val house: String = "",
    val city: AddressOption? = null,
    val street: AddressOption? = null,
    val options: List<AddressOption> = emptyList(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val failed: Boolean = false,
    val loaded: Boolean = false,
    val details: AddressDetails = AddressDetails(name = ""),
    val nameEdited: Boolean = false,
    val showReminderPicker: Boolean = false,
    val isEditing: Boolean = false,
) {
    val steps: List<InternationalStep>
        get() = (source?.access ?: sources.maxByOrNull { it.access.steps().size }?.access).steps()
    val stepIndex: Int get() = steps.indexOf(step) + 2 // Country is the first step.
    val stepCount: Int get() = steps.size + 1
    val canContinue: Boolean
        get() = !saving && when (step) {
            InternationalStep.Supplier -> source != null
            InternationalStep.Location -> query.isNotBlank()
            InternationalStep.City -> city != null
            InternationalStep.Street -> street != null
            InternationalStep.House -> city != null && street != null && house.isNotBlank()
            InternationalStep.Details -> false
        }
    val canSave: Boolean
        get() = !saving && step == InternationalStep.Details &&
                source != null && details.name.isNotBlank() && when (source.access) {
            SourceAccess.LIVE -> query.isNotBlank()
            SourceAccess.ADDRESS -> city != null && street != null && house.isNotBlank()
            else -> true
        }
}

