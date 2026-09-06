package com.lodgy.app.ui.tenant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.dao.VacantBedChoice
import com.lodgy.app.data.repository.BedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One property's vacant spaces, ready to render. Floors are already dropped for a single-unit
 *  property, so the screen never has to decide whether a floor is real (LODGY-79, LODGY-85). */
data class VacantProperty(
    val hostelId: String,
    val hostelName: String,
    val isSingleUnit: Boolean,
    val floors: List<VacantFloor>,
) {
    val choices: List<VacantBedChoice> get() = floors.flatMap { it.choices }
}

data class VacantFloor(val label: String, val choices: List<VacantBedChoice>)

data class BedPickerUiState(
    val loading: Boolean = true,
    val hasAnyProperty: Boolean = true,
    val properties: List<VacantProperty> = emptyList(),
)

/**
 * Onboarding spans every property (LODGY-85). It used to follow the selected-hostel preference,
 * which meant a warden had to go to the Property tab and switch property before they could put a
 * tenant in it — and an empty list read as "no space anywhere" when there was space next door.
 * That is the same scoping LODGY-70 took off the room view and LODGY-81 off Home.
 */
@HiltViewModel
class BedPickerViewModel @Inject constructor(
    private val bedRepository: BedRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BedPickerUiState())
    val uiState: StateFlow<BedPickerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Ordered by property, floor, room and bed in SQL, so grouping here preserves it.
            val properties = bedRepository.getVacantChoices()
                .groupBy { it.hostelId }
                .map { (hostelId, choices) ->
                    val singleUnit = choices.first().propertyType.isSingleUnit
                    VacantProperty(
                        hostelId = hostelId,
                        hostelName = choices.first().hostelName,
                        isSingleUnit = singleUnit,
                        floors = if (singleUnit) {
                            listOf(VacantFloor(label = "", choices = choices))
                        } else {
                            choices.groupBy { it.floorLabel }.map { (label, byFloor) ->
                                VacantFloor(label, byFloor)
                            }
                        },
                    )
                }
            _uiState.update {
                it.copy(
                    loading = false,
                    // Nothing vacant and nothing to be vacant are different problems: one is "you
                    // are full", the other is "you have not set up a property yet".
                    hasAnyProperty = bedRepository.hasAnyBed(),
                    properties = properties,
                )
            }
        }
    }
}
