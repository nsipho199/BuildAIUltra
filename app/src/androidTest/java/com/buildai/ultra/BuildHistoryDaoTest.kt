package com.buildai.ultra

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.buildai.ultra.data.AppDatabase
import com.buildai.ultra.data.BuildHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BuildHistoryDaoTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveBuild() = runBlocking {
        val dao = database.buildHistoryDao()
        val build = BuildHistoryEntity(
            id = "test1",
            idea = "A test app",
            status = "COMPLETE",
            downloadUrl = "http://example.com/test.apk",
            apkSize = 5000000,
            errorMessage = null,
            createdAt = System.currentTimeMillis()
        )
        dao.insertBuild(build)
        val builds = dao.getAllBuilds().first()
        assertEquals(1, builds.size)
        assertEquals("test1", builds[0].id)
        assertEquals("A test app", builds[0].idea)
        assertEquals("COMPLETE", builds[0].status)
    }

    @Test
    fun insertMultipleBuilds_returnsInReverseChronologicalOrder() = runBlocking {
        val dao = database.buildHistoryDao()
        val now = System.currentTimeMillis()
        dao.insertBuild(BuildHistoryEntity("id1", "First", "COMPLETE", null, 0, null, now - 1000))
        dao.insertBuild(BuildHistoryEntity("id2", "Second", "FAILED", null, 0, "Error", now))
        dao.insertBuild(BuildHistoryEntity("id3", "Third", "COMPLETE", "url", 100, null, now - 500))

        val builds = dao.getAllBuilds().first()
        assertEquals(3, builds.size)
        assertEquals("id2", builds[0].id)
        assertEquals("id3", builds[1].id)
        assertEquals("id1", builds[2].id)
    }

    @Test
    fun deleteBuild_removesItFromDatabase() = runBlocking {
        val dao = database.buildHistoryDao()
        dao.insertBuild(BuildHistoryEntity("id1", "Test", "COMPLETE", null, 0, null, 0))
        dao.deleteBuild("id1")
        val builds = dao.getAllBuilds().first()
        assertTrue(builds.isEmpty())
    }

    @Test
    fun clearAll_removesAllBuilds() = runBlocking {
        val dao = database.buildHistoryDao()
        dao.insertBuild(BuildHistoryEntity("id1", "Test 1", "COMPLETE", null, 0, null, 0))
        dao.insertBuild(BuildHistoryEntity("id2", "Test 2", "FAILED", null, 0, null, 0))
        dao.clearAll()
        val builds = dao.getAllBuilds().first()
        assertTrue(builds.isEmpty())
    }
}
