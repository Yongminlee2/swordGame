package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetsTest {

    private val empty = PetState()

    @Test
    fun `알을 모으면 레벨이 오르고 상한은 5다`() {
        var s = empty
        repeat(7) { s = Pets.addEgg(s, "quokka") }
        assertEquals(7, s.counts["quokka"])
        assertEquals(Pets.MAX_LEVEL, Pets.levelOf(s, "quokka"))
    }

    @Test
    fun `소유하지 않은 펫은 장착할 수 없다`() {
        val s = Pets.equip(empty, "quokka")
        assertNull(s.equippedId)
        val owned = Pets.addEgg(empty, "quokka")
        assertEquals("quokka", Pets.equip(owned, "quokka").equippedId)
    }

    @Test
    fun `null 장착은 해제다`() {
        val s = Pets.equip(Pets.addEgg(empty, "quokka"), "quokka")
        assertNull(Pets.equip(s, null).equippedId)
    }

    @Test
    fun `모든 구역에 펫이 하나씩 있다`() {
        for (zone in Zone.entries) {
            assertTrue(zone.id, Pets.run { PetKind.byZone(zone.id) } != null)
        }
        assertEquals(Zone.entries.size, PetKind.entries.size)
    }

    @Test
    fun `효과는 장착한 펫만 낸다`() {
        val owned = Pets.addEgg(Pets.addEgg(empty, "quokka"), "spriggan")
        // 장착 없음 - 전부 중립
        assertEquals(0.0, Pets.autoTapRatio(owned), 0.0)
        assertEquals(1.0, Pets.goldMultOf(owned), 0.0)
        // 쿼카 장착 - 자동 타격만
        val quokka = Pets.equip(owned, "quokka")
        assertTrue(Pets.autoTapRatio(quokka) > 0.0)
        assertEquals(1.0, Pets.goldMultOf(quokka), 0.0)
        // 요정 장착 - 골드만
        val sprig = Pets.equip(owned, "spriggan")
        assertEquals(0.0, Pets.autoTapRatio(sprig), 0.0)
        assertTrue(Pets.goldMultOf(sprig) > 1.0)
    }

    @Test
    fun `레벨이 오르면 효과가 세진다`() {
        var low = Pets.addEgg(empty, "quokka")
        low = Pets.equip(low, "quokka")
        var high = low
        repeat(4) { high = Pets.addEgg(high, "quokka") }
        assertTrue(Pets.autoTapRatio(high) > Pets.autoTapRatio(low))
        assertEquals(0.10, Pets.autoTapRatio(low), 1e-9)
        assertEquals(0.30, Pets.autoTapRatio(high), 1e-9)
    }

    @Test
    fun `펫 상태가 세이브에 왕복된다`() {
        val state = GameState(
            difficulty = Difficulty.ENDLESS,
            pets = PetState(counts = mapOf("quokka" to 3), equippedId = "quokka"),
        )
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val text = json.encodeToString(GameState.serializer(), state)
        val back = json.decodeFromString(GameState.serializer(), text)
        assertEquals(state.pets, back.pets)
    }
}
