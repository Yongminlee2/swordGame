package com.geomgang.core

/**
 * 도감에 검을 바친다.
 *
 * 예전에는 검을 얻으면 저절로 도감에 올랐다. 그래서 도감이 **지나간 자취**일 뿐
 * 플레이어가 뭘 한 게 아니었고, 아무 결정도 요구하지 않았다.
 *
 * 이제 바쳐야 열린다. 바친 검은 사라진다 — "계속 강화할까, 여기서 도감에 바칠까" 가
 * 매번 선택이 된다.
 */
object CodexOffer {

    /** 보너스가 한 계단 오르는 칸 수. */
    const val SLOT_STEP: Int = 10

    /**
     * 한 계단이 주는 몫. 0.002 는 0.2%p 다.
     *
     * v2.1에서 칸이 324→156으로 줄면서 0.1%p 그대로면 성장 축 하나가 반토막 난다.
     * 계단값을 두 배로 올려 만점을 +3.0%p 언저리에 유지한다.
     */
    const val STEP_BONUS: Double = 0.002

    /** 다 채웠을 때의 몫. 156칸이면 15계단 × 0.2%p = 3.0%p. */
    val MAX_BONUS: Double = (WeaponCatalog.ENTRIES.size / SLOT_STEP) * STEP_BONUS

    /** 이 검이 여는 도감 칸. */
    fun slotOf(sword: Sword): CodexEntry =
        WeaponCatalog.slotFor(sword.family, sword.level)

    /**
     * 아직 안 찬 칸이면 바칠 수 있다. 이미 찬 칸에 태우게 두지 않는다.
     *
     * 도감에 없는 칸(숨긴 계열)도 막는다 — 검만 사라지고 보이지도 세지지도 않는
     * 칸이 열리는 것은 함정이다.
     */
    fun canOffer(progress: ProgressState, sword: Sword): Boolean {
        val slot = slotOf(sword)
        return slot in WeaponCatalog.ENTRIES && slot !in Progress.entriesOf(progress)
    }

    /** 칸을 연다. 검을 없애는 것은 부르는 쪽(뷰모델)의 몫이다. */
    fun offer(progress: ProgressState, difficulty: Difficulty, sword: Sword): ProgressState =
        Progress.refresh(Progress.registerSword(progress, difficulty, sword))

    /** 채운 칸 수로 정해지는 보너스. 성공률과 파괴방지가 같은 크기다. */
    fun bonusOf(progress: ProgressState): ForgeBonus {
        // 노출된 칸만 센다. 옛 세이브가 채운 숨긴 계열 칸이 보너스를 부풀리면
        // 화면의 "n / 156" 과 확률이 서로 다른 말을 하게 된다.
        val entries = WeaponCatalog.ENTRIES.toSet()
        val steps = Progress.entriesOf(progress).count { it in entries } / SLOT_STEP
        val value = (steps * STEP_BONUS).coerceAtMost(MAX_BONUS)
        return ForgeBonus(successRate = value, dropGuard = value)
    }
}
