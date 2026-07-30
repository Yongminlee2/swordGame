package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 축복서와 부적은 함께 켤 수 있다 (v2.1).
 *
 * 배타는 갈림길이 아니라 함정으로 읽혔다 - 왜 하나가 꺼지는지 화면이 설명하지
 * 못했다. 이제 값(골드)이 선택을 가른다.
 */
class UsedItemsTest {

    @Test
    fun `둘 다 켤 수 있다`() {
        val items = UsedItems.NONE.toggleBlessing().toggleLuckCharm()
        assertTrue(items.blessing)
        assertTrue(items.luckCharm)
    }

    @Test
    fun `켠 것을 다시 누르면 그것만 꺼진다`() {
        val both = UsedItems(blessing = true, luckCharm = true)
        assertEquals(UsedItems(luckCharm = true), both.toggleBlessing())
        assertEquals(UsedItems(blessing = true), both.toggleLuckCharm())
    }

    @Test
    fun `아무것도 안 켠 상태에서 하나만 켜진다`() {
        assertEquals(UsedItems(blessing = true), UsedItems.NONE.toggleBlessing())
        assertEquals(UsedItems(luckCharm = true), UsedItems.NONE.toggleLuckCharm())
    }
}
