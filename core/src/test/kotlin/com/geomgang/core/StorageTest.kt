package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class StorageTest {

    private fun state(
        sword: Sword? = Sword(WeaponFamily.STRAIGHT, 5),
        storage: List<Sword> = emptyList(),
        gold: Long = 0,
        shards: Int = 0,
        pending: PendingDestroy? = null,
    ) = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = gold,
        shards = shards,
        sword = sword,
        storage = storage,
        pendingDestroy = pending,
    )

    // --- 보관 ---

    @Test
    fun `들고 있는 검을 보관하면 손이 빈다`() {
        val after = Storage.store(state())
        assertNull(after.sword)
        assertEquals(1, after.storage.size)
        assertEquals(Sword(WeaponFamily.STRAIGHT, 5), after.storage.first())
    }

    @Test
    fun `검이 없으면 보관할 수 없다`() {
        assertFalse(Storage.canStore(state(sword = null)))
    }

    @Test(expected = IllegalStateException::class)
    fun `검 없이 보관하려 하면 예외가 난다`() {
        Storage.store(state(sword = null))
    }

    @Test
    fun `보관함이 꽉 차면 더 넣을 수 없다`() {
        val full = List(Storage.CAPACITY) { Sword(WeaponFamily.CURVED, it % 10) }
        val s = state(storage = full)
        assertTrue(Storage.isFull(s))
        assertFalse(Storage.canStore(s))
    }

    // --- 장착 ---

    @Test
    fun `보관함에서 검을 꺼내 들 수 있다`() {
        val s = state(sword = null, storage = listOf(Sword(WeaponFamily.DRAGON, 12)))
        val after = Storage.equip(s, 0)
        assertEquals(Sword(WeaponFamily.DRAGON, 12), after.sword)
        assertTrue(after.storage.isEmpty())
    }

    @Test
    fun `이미 들고 있으면 자리를 맞바꾼다`() {
        val s = state(
            sword = Sword(WeaponFamily.STRAIGHT, 5),
            storage = listOf(Sword(WeaponFamily.DRAGON, 12)),
        )
        val after = Storage.equip(s, 0)
        assertEquals(Sword(WeaponFamily.DRAGON, 12), after.sword)
        assertEquals(listOf(Sword(WeaponFamily.STRAIGHT, 5)), after.storage)
    }

    @Test
    fun `보관함이 꽉 차 있어도 교체는 된다`() {
        // 맞바꾸는 것이라 총 자루 수가 늘지 않는다
        val full = List(Storage.CAPACITY) { Sword(WeaponFamily.CURVED, it % 10) }
        val s = state(sword = Sword(WeaponFamily.HOLY, 9), storage = full)
        val after = Storage.equip(s, 3)
        assertEquals(Sword(WeaponFamily.CURVED, 3), after.sword)
        assertEquals(Storage.CAPACITY, after.storage.size)
        assertTrue(Sword(WeaponFamily.HOLY, 9) in after.storage)
    }

    @Test
    fun `꺼낸 검이 최고 기록을 갱신한다`() {
        val s = state(sword = null, storage = listOf(Sword(WeaponFamily.HOLY, 17)))
        assertEquals(17, Storage.equip(s, 0).bestLevel)
    }

    @Test(expected = IllegalStateException::class)
    fun `파괴 대기 중에는 검을 바꿀 수 없다`() {
        val s = state(
            sword = null,
            storage = listOf(Sword(WeaponFamily.HOLY, 9)),
            pending = PendingDestroy(WeaponFamily.STRAIGHT, 14),
        )
        Storage.equip(s, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `없는 자리를 꺼내려 하면 예외가 난다`() {
        Storage.equip(state(), 0)
    }

    // --- 처분 ---

    @Test
    fun `보관함에서 팔면 골드가 들어온다`() {
        val sword = Sword(WeaponFamily.CURVED, 10)
        val s = state(storage = listOf(sword))
        val after = Storage.sell(s, 0)
        // 계열 배수가 붙은 값이다 - 곡도는 직검보다 조금 더 쳐 준다
        assertEquals(Economy.sellPrice(sword), after.gold)
        assertTrue(after.storage.isEmpty())
    }

    @Test
    fun `부수면 조각이 나온다`() {
        val s = state(storage = listOf(Sword(WeaponFamily.CURVED, 10)))
        val after = Storage.scrap(s, 0)
        assertTrue("조각=${after.shards}", after.shards > 0)
        assertTrue(after.storage.isEmpty())
    }

    @Test
    fun `단계가 높은 검이 부술 때 조각을 더 준다`() {
        assertTrue(
            Storage.scrapShards(Sword(WeaponFamily.CURVED, 15)) >
                Storage.scrapShards(Sword(WeaponFamily.CURVED, 3)),
        )
    }

    @Test
    fun `0단계 검도 부수면 조각이 나온다`() {
        assertTrue(Storage.scrapShards(Sword(WeaponFamily.CURVED, 0)) >= 1)
    }

    @Test
    fun `보관함이 세이브에 함께 저장된다`() {
        val s = state(storage = listOf(Sword(WeaponFamily.AXE, 7)))
        assertEquals(1, s.storage.size)
        assertEquals(WeaponFamily.AXE, s.storage.first().family)
    }

    // --- 드롭 ---

    /** 항상 드롭되게 만드는 난수. */
    private fun alwaysDrop() = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextDouble(): Double = 0.0
        override fun nextInt(from: Int, until: Int): Int = from
        override fun nextInt(until: Int): Int = 0
    }

    /** 절대 드롭되지 않게 만드는 난수. */
    private fun neverDrop() = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextDouble(): Double = 0.999
        override fun nextInt(from: Int, until: Int): Int = from
        override fun nextInt(until: Int): Int = 0
    }

    @Test
    fun `보스는 항상 검을 떨어뜨린다`() {
        val drop = SwordDrop.roll(
            zone = Zone.CAVE,
            isRare = false,
            isBoss = true,
            families = WeaponFamily.STARTERS,
            rng = neverDrop(),
        )
        assertNotNull("보스는 확률과 무관하게 떨어뜨려야 한다", drop)
    }

    @Test
    fun `확률이 빗나가면 잡몹은 아무것도 안 준다`() {
        val drop = SwordDrop.roll(Zone.CAVE, false, false, WeaponFamily.STARTERS, neverDrop())
        assertNull(drop)
    }

    @Test
    fun `떨어진 검은 해금된 계열 중 하나다`() {
        val allowed = listOf(WeaponFamily.STRAIGHT, WeaponFamily.CURVED)
        repeat(20) {
            val drop = SwordDrop.roll(Zone.CAVE, true, false, allowed, alwaysDrop())
            assertNotNull(drop)
            assertTrue(drop!!.family in allowed)
        }
    }

    @Test
    fun `고를 계열이 없으면 아무것도 안 준다`() {
        assertNull(SwordDrop.roll(Zone.CAVE, false, true, emptyList(), alwaysDrop()))
    }

    @Test
    fun `드롭 단계가 구역 권장 단계 근처다`() {
        Zone.entries.forEach { zone ->
            repeat(30) {
                val level = SwordDrop.dropLevel(zone, isBoss = false, rng = Random(it))
                assertTrue(
                    "${zone.displayName} 드롭 +$level (권장 +${zone.recommendedLevel})",
                    level in 0..RateTable.MAX_FINITE_LEVEL,
                )
                assertTrue("권장보다 너무 높다", level <= zone.recommendedLevel + 2)
            }
        }
    }

    @Test
    fun `드롭 단계는 절대 음수가 아니다`() {
        repeat(30) {
            assertTrue(SwordDrop.dropLevel(Zone.MEADOW, false, Random(it)) >= 0)
        }
    }
}
