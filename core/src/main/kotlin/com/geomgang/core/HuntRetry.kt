package com.geomgang.core

/**
 * 보스를 놓쳤을 때 골드를 내고 **즉시** 다시 도전하는 값.
 *
 * 지금까지는 놓치면 잡몹 진행(`killsInZone`)이 조용히 지워져 12마리를 다시 모아야 했다.
 * 5초에 걸린 긴장은 이 게임의 핵심이지만, 실패의 대가가 "12마리 다시"이면
 * 그 긴장이 즐거움이 아니라 부담이 된다. 재도전 문턱만 낮춘다.
 *
 * 대가를 **골드**로 받는 이유가 둘이다.
 * - 후반에 남아도는 자원이라 쓸 데가 하나 더 생긴다
 * - **재도전마다 2배**라 무한 재도전이 스스로 막힌다. 세 번째면 벌써 4배다
 *
 * 누진 카운터는 구역을 나가거나 보스를 잡으면 0으로 돌아간다.
 * 상점 강화석([GoldShop])과 같은 방식이라 규칙이 하나로 통일된다.
 */
object HuntRetry {

    /** 첫 재도전 값 = 그 구역 보스 골드 보상 ÷ 이 값. */
    const val FIRST_PRICE_DIVISOR = 2

    /** 재도전마다 붙는 배수. */
    const val GROWTH = 2

    /**
     * 값의 상한.
     *
     * 2배씩 오르므로 예순 번쯤이면 Long 을 넘겨 음수가 된다. 음수 가격은
     * "공짜로 무한 재도전" 이라는 최악의 버그가 되므로 여기서 끊는다.
     */
    const val MAX_PRICE: Long = 1_000_000_000_000_000L

    fun priceOf(zone: Zone, retries: Int): Long {
        require(retries >= 0) { "retries must be >= 0, was $retries" }
        val base = (zone.bossGold / FIRST_PRICE_DIVISOR).coerceAtLeast(1)
        var price = base
        repeat(retries) {
            if (price > MAX_PRICE / GROWTH) return MAX_PRICE
            price *= GROWTH
        }
        return price.coerceAtMost(MAX_PRICE)
    }

    fun canRetry(gold: Long, zone: Zone, retries: Int): Boolean =
        gold >= priceOf(zone, retries)
}
