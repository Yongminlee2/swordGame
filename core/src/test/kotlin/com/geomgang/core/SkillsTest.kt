package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillsTest {

    private fun sword(level: Int, family: WeaponFamily = WeaponFamily.STRAIGHT) =
        Sword(family, level)

    // --- 해금과 발동 ---

    @Test
    fun `15단계 미만에는 스킬이 없다`() {
        for (level in 0..14) {
            assertNull("+$level", Skills.roll(sword(level), skillRoll = 0.0))
        }
        assertFalse(Skills.unlocked(sword(14)))
        assertTrue(Skills.unlocked(sword(15)))
    }

    @Test
    fun `15단계부터 확률 안에서 발동한다`() {
        assertNotNull(Skills.roll(sword(15), skillRoll = 0.0))
        assertNotNull(Skills.roll(sword(15), skillRoll = Skills.CHANCE - 0.001))
        assertNull(Skills.roll(sword(15), skillRoll = Skills.CHANCE))
        assertNull(Skills.roll(sword(15), skillRoll = 0.9))
    }

    @Test
    fun `기본값 롤이면 발동하지 않는다`() {
        assertNull(Skills.roll(sword(20)))
    }

    @Test
    fun `검이 없으면 스킬도 없다`() {
        assertNull(Skills.roll(null, skillRoll = 0.0))
    }

    // --- 정의 ---

    @Test
    fun `계열 14종이 모두 자기 스킬을 갖는다`() {
        val skills = WeaponFamily.entries.map { Skills.of(it) }
        assertEquals(14, skills.size)
        assertEquals("스킬 id가 겹친다", 14, skills.map { it.id }.toSet().size)
        assertEquals("스킬 이름이 겹친다", 14, skills.map { it.name }.toSet().size)
    }

    @Test
    fun `모든 스킬은 평타보다 세다`() {
        for (family in WeaponFamily.entries) {
            val skill = Skills.of(family)
            assertTrue("${skill.name} 배수 ${skill.damageMult}", skill.damageMult > 1.0)
            assertTrue("${skill.name} 타격수", skill.hits >= 1)
        }
    }

    @Test
    fun `심판만 보스 추가 배수를 갖는다`() {
        assertEquals(2.0, Skills.of(WeaponFamily.HOLY).bossMult, 0.0)
        for (family in WeaponFamily.entries) {
            if (family == WeaponFamily.HOLY) continue
            assertEquals(1.0, Skills.of(family).bossMult, 0.0)
        }
    }

    // --- 전투 통합 ---

    @Test
    fun `스킬이 터지면 피해가 배수만큼 커진다`() {
        val s = sword(20, WeaponFamily.AXE) // 분쇄 6배
        val plain = Combat.hit(s, 0, isBoss = false, skillRoll = 1.0)
        val skilled = Combat.hit(s, 0, isBoss = false, skillRoll = 0.0)
        assertNull(plain.skill)
        assertNotNull(skilled.skill)
        assertEquals(
            Skills.of(WeaponFamily.AXE).damageMult,
            skilled.damage.toDouble() / plain.damage,
            0.05,
        )
    }

    @Test
    fun `연타 스킬은 타격 수가 늘어난다`() {
        val hit = Combat.hit(sword(20, WeaponFamily.RAPIER), 0, false, skillRoll = 0.0)
        assertEquals(4, hit.hits)
    }

    @Test
    fun `심판은 보스에게 두 배 더 아프다`() {
        val s = sword(20, WeaponFamily.HOLY)
        val onMob = Combat.hit(s, 0, isBoss = false, skillRoll = 0.0)
        val plainMob = Combat.hit(s, 0, isBoss = false, skillRoll = 1.0)
        val onBoss = Combat.hit(s, 0, isBoss = true, skillRoll = 0.0)
        val plainBoss = Combat.hit(s, 0, isBoss = true, skillRoll = 1.0)
        assertEquals(3.0, onMob.damage.toDouble() / plainMob.damage, 0.05)
        assertEquals(6.0, onBoss.damage.toDouble() / plainBoss.damage, 0.05)
    }

    @Test
    fun `사신의 낫은 최대체력 비례 피해를 더한다`() {
        val s = sword(20, WeaponFamily.SCYTHE)
        val small = Combat.hit(s, 0, false, skillRoll = 0.0, targetMaxHp = 0)
        val big = Combat.hit(s, 0, false, skillRoll = 0.0, targetMaxHp = 1_000_000)
        val expected = 1_000_000 * Skills.of(WeaponFamily.SCYTHE).maxHpRatio
        assertEquals(expected, (big.damage - small.damage).toDouble(), expected * 0.05)
    }

    @Test
    fun `스킬과 치명타는 함께 터질 수 있다`() {
        val s = sword(20, WeaponFamily.AXE)
        val both = Combat.hit(s, 0, false, critRoll = 0.0, skillRoll = 0.0)
        val skillOnly = Combat.hit(s, 0, false, critRoll = 1.0, skillRoll = 0.0)
        assertTrue(both.crit)
        assertNotNull(both.skill)
        assertEquals(
            Combat.CRIT_MULTIPLIER,
            both.damage.toDouble() / skillOnly.damage,
            0.05,
        )
    }

    @Test
    fun `스킬 없는 평타는 계열 타격 수를 그대로 쓴다`() {
        val twin = Combat.hit(sword(20, WeaponFamily.TWIN), 0, false, skillRoll = 1.0)
        assertEquals(FamilyStyle.of(WeaponFamily.TWIN).hits, twin.hits)
    }
}
