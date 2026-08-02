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

    /**
     * 칸 하나가 주는 몫. 0.0002 는 0.02%p 다.
     *
     * 계단식(10칸마다 0.2%p)이었는데 **아홉 칸을 바쳐도 숫자가 0에서 꿈쩍하지
     * 않았다** — 검을 태워 바쳤는데 아무 일도 없는 것처럼 보이면 도감은 함정으로
     * 읽힌다. 한 칸마다 오르게 바꿨다. 총량은 같다(10칸 = 0.2%p).
     */
    const val PER_SLOT_BONUS: Double = 0.0002

    /** 다 채웠을 때의 몫. 156칸 × 0.02%p = 3.12%p. */
    val MAX_BONUS: Double = WeaponCatalog.ENTRIES.size * PER_SLOT_BONUS

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

    /** 채운 칸 수로 정해지는 보너스. 성공률과 하락방지가 같은 크기다. */
    fun bonusOf(progress: ProgressState): ForgeBonus {
        // 노출된 칸만 센다. 옛 세이브가 채운 숨긴 계열 칸이 보너스를 부풀리면
        // 화면의 "n / 156" 과 확률이 서로 다른 말을 하게 된다.
        val entries = WeaponCatalog.ENTRIES.toSet()
        val filled = Progress.entriesOf(progress).count { it in entries }
        val value = (filled * PER_SLOT_BONUS).coerceAtMost(MAX_BONUS)
        return ForgeBonus(successRate = value, dropGuard = value)
    }
}
