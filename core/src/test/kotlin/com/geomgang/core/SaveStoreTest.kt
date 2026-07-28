package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SaveStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun store() = SaveStore(tmp.root)

    private fun sample() = GameState(
        difficulty = Difficulty.NORMAL,
        gold = 12_345,
        shards = 42,
        sword = Sword(WeaponFamily.DRAGON, 13),
        inventory = Inventory(preventTickets = 2, blessingScrolls = 1),
        bestLevel = 15,
    )

    @Test
    fun `저장한 적 없는 모드는 빈 상태로 시작한다`() {
        val fresh = store().loadGame(Difficulty.NORMAL)
        assertEquals(Difficulty.NORMAL, fresh.difficulty)
        assertEquals(0L, fresh.gold)
        assertNull(fresh.sword)
    }

    @Test
    fun `저장한 상태가 그대로 복원된다`() {
        val s = store()
        s.saveGame(sample())
        assertEquals(sample(), s.loadGame(Difficulty.NORMAL))
    }

    @Test
    fun `uniqueId와 정수가 없던 옛 세이브도 그대로 열린다`() {
        // v1.2 이전 형식의 JSON 을 직접 심는다 - 새 필드(uniqueId·essences·quests)가 없다.
        val old = """
            {"difficulty":"NORMAL","gold":500,"shards":3,
             "sword":{"family":"STRAIGHT","level":7},
             "inventory":{},"bestLevel":7}
        """.trimIndent()
        File(tmp.root, "save_normal.json").writeText(old)
        val loaded = store().loadGame(Difficulty.NORMAL)
        assertEquals(500L, loaded.gold)
        assertEquals(Sword(WeaponFamily.STRAIGHT, 7), loaded.sword)
        assertNull(loaded.sword!!.uniqueId)
        assertTrue(loaded.essences.isEmpty())
    }

    @Test
    fun `고유검과 정수가 저장되고 복원된다`() {
        val s = store()
        val state = sample().copy(
            sword = Sword(WeaponFamily.HOLY, 12, stars = 1, uniqueId = "trinity"),
            essences = mapOf("volcano" to 3),
        )
        s.saveGame(state)
        val loaded = s.loadGame(Difficulty.NORMAL)
        assertEquals("trinity", loaded.sword!!.uniqueId)
        assertEquals(3, loaded.essences["volcano"])
    }

    @Test
    fun `파괴 대기 상태도 저장되고 복원된다`() {
        val s = store()
        val pending = sample().copy(
            sword = null,
            pendingDestroy = PendingDestroy(WeaponFamily.HOLY, 17),
        )
        s.saveGame(pending)
        assertEquals(
            PendingDestroy(WeaponFamily.HOLY, 17),
            s.loadGame(Difficulty.NORMAL).pendingDestroy,
        )
    }

    @Test
    fun `모드마다 파일이 분리된다`() {
        val s = store()
        s.saveGame(sample())
        s.saveGame(GameState(Difficulty.HARD, gold = 7))
        assertEquals(12_345L, s.loadGame(Difficulty.NORMAL).gold)
        assertEquals(7L, s.loadGame(Difficulty.HARD).gold)
    }

    @Test
    fun `진행도가 저장되고 복원된다`() {
        val s = store()
        val p = ProgressState(
            codex = setOf(CodexKey(WeaponFamily.TWIN, 9, Difficulty.EASY)),
            achievements = setOf(Achievement.REACH_10),
            selectedTitle = Achievement.REACH_10,
            stats = Stats(attempts = 300, successes = 120, attemptsByLevel = mapOf(10 to 40L)),
        )
        s.saveProgress(p)
        assertEquals(p, s.loadProgress())
    }

    /**
     * 티어가 칸이던 시절 파일을 그대로 읽는다.
     *
     * 불러오는 문에서 이관해야 한다. 예전에는 [Progress.refresh] 에 맡겼는데 그 함수를
     * 거치지 않는 경로가 있어, 단계가 안 적힌 기록이 화면까지 가서 도감이 계열마다
     * **한 칸**으로 뭉쳤다. 이 테스트가 그 길을 막는다.
     */
    @Test
    fun `옛 티어 세이브를 읽으면 단계 칸으로 이관된다`() {
        File(tmp.root, SaveStore.PROGRESS_FILE).writeText(
            """
            {
              "codex": [
                {"family":"STRAIGHT","tier":"RUSTY","difficulty":"EASY"},
                {"family":"STRAIGHT","tier":"STEEL","difficulty":"EASY"},
                {"family":"CURVED","tier":"RUSTY","difficulty":"EASY"}
              ]
            }
            """.trimIndent(),
        )

        val loaded = store().loadProgress()
        val entries = Progress.entriesOf(loaded)

        // 계열마다 한 칸으로 뭉치지 않고 티어 수만큼 살아남는다
        assertEquals(3, entries.size)
        assertTrue(CodexEntry(WeaponFamily.STRAIGHT, 0) in entries)
        assertTrue(CodexEntry(WeaponFamily.STRAIGHT, 3) in entries)
        assertTrue(CodexEntry(WeaponFamily.CURVED, 0) in entries)
        // 단계가 안 적힌 기록이 남아 있으면 안 된다
        assertTrue(loaded.codex.none { it.level == CodexKey.LEGACY_LEVEL })
    }

    @Test
    fun `두 번째 저장이 백업 파일을 남긴다`() {
        val s = store()
        s.saveGame(sample())
        s.saveGame(sample().copy(gold = 999))
        assertTrue(File(tmp.root, "save_normal.json.bak").exists())
        assertEquals(999L, s.loadGame(Difficulty.NORMAL).gold)
    }

    @Test
    fun `본 파일이 깨지면 백업으로 복원한다`() {
        val s = store()
        s.saveGame(sample())
        s.saveGame(sample().copy(gold = 999))
        File(tmp.root, "save_normal.json").writeText("{ 이건 JSON 이 아니다")
        // 백업에는 첫 저장(12,345골드)이 들어 있다
        assertEquals(12_345L, s.loadGame(Difficulty.NORMAL).gold)
    }

    @Test
    fun `본 파일과 백업이 모두 깨지면 빈 상태로 시작한다`() {
        val s = store()
        s.saveGame(sample())
        s.saveGame(sample().copy(gold = 999))
        File(tmp.root, "save_normal.json").writeText("깨짐")
        File(tmp.root, "save_normal.json.bak").writeText("이것도 깨짐")
        val recovered = s.loadGame(Difficulty.NORMAL)
        assertEquals(0L, recovered.gold)
        assertEquals(Difficulty.NORMAL, recovered.difficulty)
    }

    @Test
    fun `쓰다 만 임시 파일이 남아 있어도 읽기에 지장이 없다`() {
        val s = store()
        s.saveGame(sample())
        File(tmp.root, "save_normal.json.tmp").writeText("쓰다 만 것")
        assertEquals(12_345L, s.loadGame(Difficulty.NORMAL).gold)
    }

    @Test
    fun `모드 초기화는 그 모드만 지운다`() {
        val s = store()
        s.saveGame(sample())
        s.saveGame(GameState(Difficulty.HARD, gold = 7))
        s.saveProgress(ProgressState(achievements = setOf(Achievement.REACH_10)))

        s.resetGame(Difficulty.NORMAL)

        assertEquals(0L, s.loadGame(Difficulty.NORMAL).gold)
        assertEquals(7L, s.loadGame(Difficulty.HARD).gold)
        // 도감·업적은 초기화의 영향을 받지 않는다. 이게 이 게임의 재도전 동력이다.
        assertTrue(Achievement.REACH_10 in s.loadProgress().achievements)
    }

    @Test
    fun `초기화는 백업 파일까지 지운다`() {
        val s = store()
        s.saveGame(sample())
        s.saveGame(sample().copy(gold = 999))
        s.resetGame(Difficulty.NORMAL)
        assertEquals(0L, s.loadGame(Difficulty.NORMAL).gold)
    }

    @Test
    fun `저장 디렉터리가 없으면 만들어 쓴다`() {
        val nested = File(tmp.root, "a/b/c")
        val s = SaveStore(nested)
        s.saveGame(sample())
        assertNotNull(s.loadGame(Difficulty.NORMAL).sword)
    }
}
