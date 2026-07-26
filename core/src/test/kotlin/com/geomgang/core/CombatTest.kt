package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombatTest {

    private fun sword(level: Int, family: WeaponFamily = WeaponFamily.STRAIGHT) =
        Sword(family, level)

    // --- 공격력 ---

    @Test
    fun `검이 없으면 공격력이 0이다`() {
        assertEquals(0L, Combat.attackPower(null))
    }

    @Test
    fun `강화 단계가 오르면 공격력이 오른다`() {
        for (level in 1..20) {
            assertTrue(
                "+$level",
                Combat.attackPower(sword(level)) > Combat.attackPower(sword(level - 1)),
            )
        }
    }

    @Test
    fun `대검은 직검보다 한 방이 세고 세검은 약하다`() {
        val level = 10
        val straight = Combat.attackPower(sword(level, WeaponFamily.STRAIGHT))
        val great = Combat.attackPower(sword(level, WeaponFamily.GREAT))
        val rapier = Combat.attackPower(sword(level, WeaponFamily.RAPIER))
        assertTrue(great > straight)
        assertTrue(rapier < straight)
    }

    @Test
    fun `세검은 대검보다 빠르게 칠 수 있다`() {
        assertTrue(
            Combat.minTapMillis(sword(5, WeaponFamily.RAPIER)) <
                Combat.minTapMillis(sword(5, WeaponFamily.GREAT)),
        )
    }

    // --- 계열 특성 ---

    @Test
    fun `쌍검은 한 번 탭에 두 번 들어간다`() {
        val hit = Combat.hit(sword(5, WeaponFamily.TWIN), combo = 0, isBoss = false)
        assertEquals(2, hit.hits)
        assertTrue(hit.damage > 0)
    }

    @Test
    fun `곡도는 연속으로 칠수록 세진다`() {
        val s = sword(10, WeaponFamily.CURVED)
        val first = Combat.hit(s, combo = 0, isBoss = false).damage
        val later = Combat.hit(s, combo = 10, isBoss = false).damage
        assertTrue("$first -> $later", later > first)
    }

    @Test
    fun `연속 타격 보너스에 상한이 있다`() {
        val s = sword(10, WeaponFamily.CURVED)
        val capped = Combat.hit(s, combo = 1_000, isBoss = false).damage
        val atCap = Combat.hit(s, combo = 100, isBoss = false).damage
        assertEquals(atCap, capped)
    }

    @Test
    fun `직검은 연속으로 쳐도 세지지 않는다`() {
        val s = sword(10, WeaponFamily.STRAIGHT)
        assertEquals(
            Combat.hit(s, combo = 0, isBoss = false).damage,
            Combat.hit(s, combo = 50, isBoss = false).damage,
        )
    }

    @Test
    fun `성검은 보스에게 더 큰 피해를 준다`() {
        val s = sword(10, WeaponFamily.HOLY)
        val normal = Combat.hit(s, combo = 0, isBoss = false).damage
        val boss = Combat.hit(s, combo = 0, isBoss = true).damage
        assertTrue("$normal -> $boss", boss > normal)
    }

    @Test
    fun `직검은 보스라고 더 세지 않는다`() {
        val s = sword(10, WeaponFamily.STRAIGHT)
        assertEquals(
            Combat.hit(s, combo = 0, isBoss = false).damage,
            Combat.hit(s, combo = 0, isBoss = true).damage,
        )
    }

    @Test
    fun `용검만 화상 피해가 있다`() {
        assertTrue(Combat.burnPerSecond(sword(10, WeaponFamily.DRAGON)) > 0)
        WeaponFamily.entries.filter { it != WeaponFamily.DRAGON }.forEach {
            assertEquals("${it.id} 화상", 0L, Combat.burnPerSecond(sword(10, it)))
        }
    }

    @Test
    fun `마검은 조각을 더 얻는다`() {
        val base = 4
        assertTrue(
            Combat.shardReward(sword(10, WeaponFamily.DEMON), base) >
                Combat.shardReward(sword(10, WeaponFamily.STRAIGHT), base),
        )
    }

    @Test
    fun `조각이 안 나오는 구역에서는 마검도 못 얻는다`() {
        assertEquals(0, Combat.shardReward(sword(10, WeaponFamily.DEMON), base = 0))
    }

    @Test
    fun `계열 8종이 서로 다른 전투 방식을 갖는다`() {
        val styles = WeaponFamily.entries.map { FamilyStyle.of(it) }
        assertEquals(8, styles.size)
        assertEquals("같은 전투 방식을 쓰는 계열이 있다", 8, styles.toSet().size)
    }

    // --- 보스 문턱 ---

    @Test
    fun `구역마다 권장 단계가 오른다`() {
        val zones = Zone.entries
        for (i in 1 until zones.size) {
            assertTrue(zones[i].recommendedLevel > zones[i - 1].recommendedLevel)
        }
    }

    @Test
    fun `약한 검으로는 보스를 시간 안에 못 잡는다`() {
        // 초원 보스를 +0 검으로는 잡을 수 있어야 하고, 화산 보스는 무리여야 한다
        assertFalse(Combat.canBeatBoss(sword(0), Zone.VOLCANO))
        assertFalse(Combat.canBeatBoss(sword(0), Zone.DRAGON_NEST))
    }

    @Test
    fun `권장 단계면 그 구역 보스를 잡을 수 있다`() {
        Zone.entries.forEach { zone ->
            assertTrue(
                "${zone.displayName} 보스를 +${zone.recommendedLevel} 로 못 잡는다",
                Combat.canBeatBoss(sword(zone.recommendedLevel + 2), zone),
            )
        }
    }

    @Test
    fun `검이 없으면 보스를 잡을 수 없다`() {
        assertFalse(Combat.canBeatBoss(null, Zone.MEADOW))
    }

    // --- 진행 상태 ---

    @Test
    fun `첫 구역은 처음부터 열려 있고 나머지는 잠겨 있다`() {
        val fresh = AdventureState()
        assertTrue(fresh.isUnlocked(Zone.MEADOW))
        assertFalse(fresh.isUnlocked(Zone.CAVE))
        assertFalse(fresh.isUnlocked(Zone.DRAGON_NEST))
    }

    @Test
    fun `앞 구역을 깨면 다음 구역이 열린다`() {
        val state = AdventureState(clearedZoneIds = setOf(Zone.MEADOW.id))
        assertTrue(state.isUnlocked(Zone.CAVE))
        assertFalse(state.isUnlocked(Zone.VOLCANO))
    }

    @Test
    fun `잡몹을 정해진 수만큼 잡으면 보스가 나온다`() {
        assertFalse(AdventureState(killsInZone = 9).bossReady)
        assertTrue(AdventureState(killsInZone = Zone.MONSTERS_BEFORE_BOSS).bossReady)
    }

    @Test
    fun `사냥 진행이 세이브에 함께 저장된다`() {
        val state = GameState(
            Difficulty.NORMAL,
            adventure = AdventureState(
                zoneId = Zone.CAVE.id,
                killsInZone = 4,
                clearedZoneIds = setOf(Zone.MEADOW.id),
            ),
        )
        assertEquals(Zone.CAVE, state.adventure.zone)
        assertEquals(4, state.adventure.killsInZone)
    }
}
