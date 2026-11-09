package org.exxlexxlee.svitlo.sync

internal enum class RefreshOutcome { Success, Retry, Failure }

// A periodic run gets at most three attempts, including for partial failures.
internal fun refreshOutcome(results: Collection<Result<Unit>>, attempt: Int): RefreshOutcome = when {
    results.none { it.isFailure } -> RefreshOutcome.Success
    attempt < 2 -> RefreshOutcome.Retry
    else -> RefreshOutcome.Failure
}
