package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 축복서와 부적은 함께 켜지지 않는다.
 *
 * 둘 다 쓸 수 있으면 "있으면 전부 켠다" 가 유일한 최선이 되어 선택이 사라진다.
 * 하나만 고르게 해야 **확률을 올릴까, 검을 지킬까** 가 매 판 갈림길이 된다.
 */
class UsedItemsTest {

    @Test
    fun `축복서를 켜면 부적이 꺼진다`() {
        val items = UsedItems(luckCharm = true).toggleBlessing()
        assertTrue(items.blessing)
        assertFalse(items.luckCharm)
    }

    @Test
    fun `부적을 켜면 축복서가 꺼진다`() {
        val items = UsedItems(blessing = true).toggleLuckCharm()
        assertTrue(items.luckCharm)
        assertFalse(items.blessing)
    }

    @Test
    fun `켠 것을 다시 누르면 둘 다 꺼진다`() {
        assertEquals(UsedItems.NONE, UsedItems(blessing = true).toggleBlessing())
        assertEquals(UsedItems.NONE, UsedItems(luckCharm = true).toggleLuckCharm())
    }

    @Test
    fun `아무것도 안 켠 상태에서 하나만 켜진다`() {
        assertEquals(UsedItems(blessing = true), UsedItems.NONE.toggleBlessing())
        assertEquals(UsedItems(luckCharm = true), UsedItems.NONE.toggleLuckCharm())
    }
}
