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
     * 어느 칸이든 주는 바탕 몫. 0.0001 은 0.01%p 다.
     *
     * 계단식(10칸마다 0.2%p)이었는데 **아홉 칸을 바쳐도 숫자가 0에서 꿈쩍하지
     * 않았다** — 검을 태워 바쳤는데 아무 일도 없는 것처럼 보이면 도감은 함정으로
     * 읽힌다. 바탕 몫이 있어 어떤 칸을 채워도 화면의 소수 둘째 자리가 움직인다.
     */
    const val PER_SLOT_BONUS: Double = 0.0001

    /**
     * 칸의 **단계당** 더해지는 몫. 0.00002 는 0.002%p 다.
     *
     * 같은 한 칸이라도 +20 검은 +0 검보다 훨씬 비싸다 — 바치는 값이 다른데
     * 보너스가 같으면 깊은 칸을 채울 이유가 흐려진다. +0 = 0.01%p,
     * +10 = 0.03%p, +20 = 0.05%p. 도감 만점은 약 +5%p 로, 균일 0.02%p
     * 시절(+3.12%p)보다 커졌다 — 깊은 칸의 값을 쳐 준 만큼이다.
     */
    const val PER_LEVEL_BONUS: Double = 0.00002

    /** 칸 하나의 몫 = 바탕 + 깊이. */
    fun slotBonus(entry: CodexEntry): Double =
        PER_SLOT_BONUS + entry.level * PER_LEVEL_BONUS

    /** 다 채웠을 때의 몫. */
    val MAX_BONUS: Double = WeaponCatalog.ENTRIES.sumOf { slotBonus(it) }

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

    /** 채운 칸들의 몫 합. 성공률과 하락방지가 같은 크기다. */
    fun bonusOf(progress: ProgressState): ForgeBonus {
        // 노출된 칸만 센다. 옛 세이브가 채운 숨긴 계열 칸이 보너스를 부풀리면
        // 화면의 "n / 156" 과 확률이 서로 다른 말을 하게 된다.
        val entries = WeaponCatalog.ENTRIES.toSet()
        val value = Progress.entriesOf(progress)
            .filter { it in entries }
            .sumOf { slotBonus(it) }
            .coerceAtMost(MAX_BONUS)
        return ForgeBonus(successRate = value, dropGuard = value)
    }
}
