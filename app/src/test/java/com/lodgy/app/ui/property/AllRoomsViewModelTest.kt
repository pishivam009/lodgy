package com.lodgy.app.ui.property

import androidx.lifecycle.SavedStateHandle
import com.lodgy.app.data.dao.RoomOccupancy
import com.lodgy.app.ui.common.RoomFill
import com.lodgy.app.data.dao.RoomWithFloor
import com.lodgy.app.data.entity.Hostel
import com.lodgy.app.data.entity.RoomType
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.HostelRepository
import com.lodgy.app.data.repository.RoomRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AllRoomsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val roomRepository: RoomRepository = mockk()
    private val bedRepository: BedRepository = mockk()
    private val hostelRepository: HostelRepository = mockk()

    private fun room(id: String, number: String, floor: String, hostelId: String = "h1", hostelName: String = "Sunrise") =
        RoomWithFloor(roomId = id, roomNumber = number, type = RoomType.DOUBLE, pricePerBed = 3000.0, floorId = "f-$floor", floorLabel = floor, hostelId = hostelId, hostelName = hostelName)

    private fun hostel(id: String, name: String) =
        Hostel(id = id, wardenId = "w1", name = name, address = "", contactPhone = "", createdAt = 0L, updatedAt = 0L)

    private fun viewModel(
        rooms: List<RoomWithFloor>,
        occupancy: List<RoomOccupancy> = emptyList(),
        hostels: List<Hostel> = listOf(hostel("h1", "Sunrise")),
        args: Map<String, Any?> = emptyMap(),
    ): AllRoomsViewModel {
        every { roomRepository.getAllWithFloor() } returns flowOf(rooms)
        every { bedRepository.observeRoomOccupancy() } returns flowOf(occupancy)
        every { hostelRepository.getAll() } returns flowOf(hostels)
        return AllRoomsViewModel(roomRepository, bedRepository, hostelRepository, SavedStateHandle(args))
    }

    @Test
    fun `lists every room across floors, each labelled with its floor`() {
        val state = viewModel(listOf(room("r1", "101", "Ground"), room("r2", "201", "First"))).uiState.value

        assertEquals(listOf("Sunrise"), state.hostels.map { it.name })
        assertEquals(listOf("Ground", "First"), state.items.map { it.room.floorLabel })
        assertEquals(listOf("101", "201"), state.items.map { it.room.roomNumber })
    }

    @Test
    fun `bed counts attach to their room, and a room with no beds reports zero`() {
        val state = viewModel(
            rooms = listOf(room("r1", "101", "Ground"), room("r2", "102", "Ground")),
            occupancy = listOf(RoomOccupancy("r1", totalBeds = 3, occupiedBeds = 1)),
        ).uiState.value

        assertEquals(2, state.items.first().vacantBeds)
        assertEquals(0, state.items.last().totalBeds)
    }

    @Test
    fun `summary counts each room into exactly one bucket`() {
        val state = viewModel(
            rooms = listOf(
                room("r1", "101", "Ground"),
                room("r2", "102", "Ground"),
                room("r3", "103", "Ground"),
                room("r4", "104", "Ground"),
            ),
            occupancy = listOf(
                RoomOccupancy("r1", totalBeds = 2, occupiedBeds = 0),
                RoomOccupancy("r2", totalBeds = 2, occupiedBeds = 1),
                RoomOccupancy("r3", totalBeds = 2, occupiedBeds = 2),
                RoomOccupancy("r4", totalBeds = 0, occupiedBeds = 0),
            ),
        ).uiState.value

        assertEquals(2, state.emptyRooms)
        assertEquals(1, state.partialRooms)
        assertEquals(1, state.fullRooms)
        assertEquals(state.items.size, state.emptyRooms + state.partialRooms + state.fullRooms)
    }

    @Test
    fun `each room carries the fill state its tile is coloured by`() {
        val state = viewModel(
            rooms = listOf(room("r1", "101", "Ground"), room("r2", "102", "Ground")),
            occupancy = listOf(
                RoomOccupancy("r1", totalBeds = 2, occupiedBeds = 2),
                RoomOccupancy("r2", totalBeds = 2, occupiedBeds = 1),
            ),
        ).uiState.value

        assertEquals(RoomFill.FULL, state.items.first().occupancy)
        assertEquals(RoomFill.PARTIAL, state.items.last().occupancy)
    }

    /** LODGY-70: the screen was hostel-scoped by construction, so surveying the estate meant
     *  opening it once per property. */
    @Test
    fun `every hostel's rooms are listed by default, and the hostel filter narrows`() {
        val viewModel = viewModel(
            rooms = listOf(
                room("r1", "101", "Ground"),
                room("r2", "201", "Ground", hostelId = "h2", hostelName = "Moonlight"),
            ),
            hostels = listOf(hostel("h1", "Sunrise"), hostel("h2", "Moonlight")),
        )

        assertEquals(listOf("101", "201"), viewModel.uiState.value.visibleItems.map { it.room.roomNumber })

        viewModel.onHostelFilterChange("h2")
        assertEquals(listOf("201"), viewModel.uiState.value.visibleItems.map { it.room.roomNumber })
        assertEquals("Moonlight", viewModel.uiState.value.filterHostelName)

        viewModel.onHostelFilterChange(null)
        assertEquals(2, viewModel.uiState.value.visibleItems.size)
    }

    /** LODGY-72: "has space" must include partly filled rooms - one still has a bed free, and
     *  hiding it would answer "where can I put someone" wrongly. */
    @Test
    fun `the space filter keeps partly filled rooms, not just wholly empty ones`() {
        val viewModel = viewModel(
            rooms = listOf(room("r1", "101", "G"), room("r2", "102", "G"), room("r3", "103", "G")),
            occupancy = listOf(
                RoomOccupancy("r1", totalBeds = 2, occupiedBeds = 0),
                RoomOccupancy("r2", totalBeds = 2, occupiedBeds = 1),
                RoomOccupancy("r3", totalBeds = 2, occupiedBeds = 2),
            ),
        )

        viewModel.onSpaceFilterChange(RoomSpaceFilter.HAS_SPACE)
        assertEquals(listOf("101", "102"), viewModel.uiState.value.visibleItems.map { it.room.roomNumber })
    }

    /** The summary counts what is on screen, so it can never contradict the tiles under it. */
    @Test
    fun `the summary follows the active filters`() {
        val viewModel = viewModel(
            rooms = listOf(
                room("r1", "101", "G"),
                room("r2", "201", "G", hostelId = "h2", hostelName = "Moonlight"),
            ),
            occupancy = listOf(
                RoomOccupancy("r1", totalBeds = 2, occupiedBeds = 0),
                RoomOccupancy("r2", totalBeds = 2, occupiedBeds = 2),
            ),
            hostels = listOf(hostel("h1", "Sunrise"), hostel("h2", "Moonlight")),
        )

        assertEquals(1, viewModel.uiState.value.emptyRooms)
        assertEquals(1, viewModel.uiState.value.fullRooms)

        viewModel.onHostelFilterChange("h2")
        assertEquals(0, viewModel.uiState.value.emptyRooms)
        assertEquals(1, viewModel.uiState.value.fullRooms)
    }

    /** Arriving from the Home vacant-beds tile must land already filtered. */
    @Test
    fun `the hasSpace argument pre-filters on arrival`() {
        val viewModel = viewModel(
            rooms = listOf(room("r1", "101", "G"), room("r2", "102", "G")),
            occupancy = listOf(
                RoomOccupancy("r1", totalBeds = 2, occupiedBeds = 2),
                RoomOccupancy("r2", totalBeds = 2, occupiedBeds = 0),
            ),
            args = mapOf("hasSpace" to true),
        )

        assertEquals(RoomSpaceFilter.HAS_SPACE, viewModel.uiState.value.spaceFilter)
        assertEquals(listOf("102"), viewModel.uiState.value.visibleItems.map { it.room.roomNumber })
    }

    /** The tile that leads here counts BEDS while this screen counts ROOMS - 2 rooms with space
     *  can hold 3 free beds - so the bed figure is exposed to keep the two reconcilable. */
    @Test
    fun `vacant beds in view is counted separately from rooms`() {
        val viewModel = viewModel(
            rooms = listOf(room("r1", "101", "G"), room("r2", "102", "G")),
            occupancy = listOf(
                RoomOccupancy("r1", totalBeds = 2, occupiedBeds = 0),
                RoomOccupancy("r2", totalBeds = 2, occupiedBeds = 1),
            ),
        )

        assertEquals(2, viewModel.uiState.value.visibleItems.size)
        assertEquals(3, viewModel.uiState.value.vacantBedsInView)
    }
}
