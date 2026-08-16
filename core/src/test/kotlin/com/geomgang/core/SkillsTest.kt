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
    fun `용검은 1단계부터 스킬을 쓴다`() {
        val zero = sword(0, WeaponFamily.DRAGON)
        val one = sword(1, WeaponFamily.DRAGON)
        assertFalse(Skills.unlocked(zero))
        assertNull(Skills.roll(zero, skillRoll = 0.0))
        assertTrue(Skills.unlocked(one))
        assertNotNull(Skills.roll(one, skillRoll = 0.0))
    }

    @Test
    fun `용검은 확률 안에서만 발동한다`() {
        val dragon = sword(1, WeaponFamily.DRAGON)
        assertNotNull(Skills.roll(dragon, skillRoll = 0.0))
        assertNotNull(Skills.roll(dragon, skillRoll = Skills.CHANCE - 0.001))
        assertNull(Skills.roll(dragon, skillRoll = Skills.CHANCE))
        assertNull(Skills.roll(dragon, skillRoll = 0.9))
    }

    @Test
    fun `원래 계열은 고강화여도 스킬을 쓰지 않는다`() {
        for (family in WeaponFamily.entries - WeaponFamily.DRAGON) {
            val sourceSword = sword(50, family)
            assertFalse(family.name, Skills.unlocked(sourceSword))
            assertNull(family.name, Skills.roll(sourceSword, skillRoll = 0.0))
        }
    }

    @Test
    fun `용검은 강화 구간마다 일반 계열 스킬을 순서대로 계승한다`() {
        val expected = mapOf(
            1 to WeaponFamily.CURVED,
            7 to WeaponFamily.CURVED,
            8 to WeaponFamily.STRAIGHT,
            14 to WeaponFamily.STRAIGHT,
            15 to WeaponFamily.DEMON,
            20 to WeaponFamily.DEMON,
            21 to WeaponFamily.RAPIER,
            27 to WeaponFamily.RAPIER,
            28 to WeaponFamily.GREAT,
            34 to WeaponFamily.GREAT,
            35 to WeaponFamily.HOLY,
            41 to WeaponFamily.HOLY,
            42 to WeaponFamily.DRAGON,
            50 to WeaponFamily.DRAGON,
        )
        expected.forEach { (level, family) ->
            assertEquals("+$level", Skills.of(family), Skills.of(sword(level, WeaponFamily.DRAGON)))
        }
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
        val s = sword(28, WeaponFamily.DRAGON) // 대검에서 계승한 붕괴 5배
        val plain = Combat.hit(s, 0, isBoss = false, skillRoll = 1.0)
        val skilled = Combat.hit(s, 0, isBoss = false, skillRoll = 0.0)
        assertNull(plain.skill)
        assertNotNull(skilled.skill)
        assertEquals(
            Skills.of(WeaponFamily.GREAT).damageMult,
            skilled.damage.toDouble() / plain.damage,
            0.05,
        )
    }

    @Test
    fun `연타 스킬은 타격 수가 늘어난다`() {
        val hit = Combat.hit(sword(21, WeaponFamily.DRAGON), 0, false, skillRoll = 0.0)
        assertEquals(4, hit.hits)
    }

    @Test
    fun `심판은 보스에게 두 배 더 아프다`() {
        val s = sword(35, WeaponFamily.DRAGON)
        val onMob = Combat.hit(s, 0, isBoss = false, skillRoll = 0.0)
        val plainMob = Combat.hit(s, 0, isBoss = false, skillRoll = 1.0)
        val onBoss = Combat.hit(s, 0, isBoss = true, skillRoll = 0.0)
        val plainBoss = Combat.hit(s, 0, isBoss = true, skillRoll = 1.0)
        assertEquals(3.0, onMob.damage.toDouble() / plainMob.damage, 0.05)
        assertEquals(6.0, onBoss.damage.toDouble() / plainBoss.damage, 0.05)
    }

    @Test
    fun `원래 검은 스킬 롤이 성공해도 평타다`() {
        val sourceSword = sword(50, WeaponFamily.GREAT)
        val hit = Combat.hit(sourceSword, 0, false, skillRoll = 0.0)
        assertNull(hit.skill)
    }

    @Test
    fun `스킬과 치명타는 함께 터질 수 있다`() {
        val s = sword(28, WeaponFamily.DRAGON)
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
