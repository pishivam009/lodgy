package com.lodgy.app.ui.tenant

import com.lodgy.app.data.dao.VacantBedChoice
import com.lodgy.app.data.entity.PropertyType
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BedPickerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bedRepository: BedRepository = mockk()

    private fun choice(
        bedId: String,
        bedLabel: String = "A",
        roomNumber: String = "101",
        floorLabel: String = "Ground",
        hostelId: String = "h1",
        hostelName: String = "Sunrise PG",
        propertyType: PropertyType = PropertyType.HOSTEL,
    ) = VacantBedChoice(bedId, bedLabel, roomNumber, floorLabel, hostelId, hostelName, propertyType)

    private fun viewModel(choices: List<VacantBedChoice>, anyBed: Boolean = true): BedPickerViewModel {
        coEvery { bedRepository.getVacantChoices() } returns choices
        coEvery { bedRepository.hasAnyBed() } returns anyBed
        return BedPickerViewModel(bedRepository)
    }

    /** LODGY-85. The picker used to follow the selected-hostel preference, so a warden had to switch
     *  property before they could put a tenant in it. */
    @Test
    fun `every property is offered, not just one`() {
        val state = viewModel(
            listOf(
                choice("b1"),
                choice("b2", hostelId = "h2", hostelName = "Moonlight", roomNumber = "501", floorLabel = "Fifth"),
            ),
        ).uiState.value

        assertFalse(state.loading)
        assertEquals(listOf("Sunrise PG", "Moonlight"), state.properties.map { it.hostelName })
        assertEquals(listOf("b1", "b2"), state.properties.flatMap { p -> p.choices.map { it.bedId } })
    }

    @Test
    fun `a hostel keeps its floors, grouped in the order the query returned`() {
        val property = viewModel(
            listOf(
                choice("b1", floorLabel = "Ground", roomNumber = "101"),
                choice("b2", floorLabel = "Ground", roomNumber = "102"),
                choice("b3", floorLabel = "First", roomNumber = "201"),
            ),
        ).uiState.value.properties.single()

        assertFalse(property.isSingleUnit)
        assertEquals(listOf("Ground", "First"), property.floors.map { it.label })
        assertEquals(listOf("b1", "b2"), property.floors.first().choices.map { it.bedId })
    }

    /** A shop's floor is the placeholder its hierarchy carries, never something the warden named,
     *  so it must not be grouped under one (LODGY-79). */
    @Test
    fun `a single-unit property is one choice with no floor`() {
        val property = viewModel(
            listOf(
                choice(
                    "b9", bedLabel = "A", roomNumber = "Corner shop", floorLabel = "-",
                    hostelId = "h9", hostelName = "Corner shop", propertyType = PropertyType.SHOP,
                ),
            ),
        ).uiState.value.properties.single()

        assertTrue(property.isSingleUnit)
        assertEquals(1, property.floors.size)
        assertEquals("", property.floors.single().label)
        assertEquals("b9", property.choices.single().bedId)
    }

    @Test
    fun `a warden with both kinds of property gets both, each grouped its own way`() {
        val state = viewModel(
            listOf(
                choice("b1", floorLabel = "Ground"),
                choice(
                    "b9", roomNumber = "Corner shop", floorLabel = "-",
                    hostelId = "h9", hostelName = "Corner shop", propertyType = PropertyType.SHOP,
                ),
            ),
        ).uiState.value

        assertEquals(2, state.properties.size)
        assertFalse(state.properties.first().isSingleUnit)
        assertTrue(state.properties.last().isSingleUnit)
    }

    /** "You are full" and "you have not set up a property yet" are different problems and the
     *  picker says which. */
    @Test
    fun `a full portfolio is empty but still has properties`() {
        val state = viewModel(emptyList(), anyBed = true).uiState.value

        assertTrue(state.properties.isEmpty())
        assertTrue(state.hasAnyProperty)
    }

    @Test
    fun `no property at all is reported separately`() {
        val state = viewModel(emptyList(), anyBed = false).uiState.value

        assertTrue(state.properties.isEmpty())
        assertFalse(state.hasAnyProperty)
    }
}
