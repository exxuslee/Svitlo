package org.exxlexxlee.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.exxlexxlee.data.local.AppDatabase
import org.exxlexxlee.data.local.MIGRATION_1_2
import org.exxlexxlee.data.local.toDomain
import org.exxlexxlee.data.repository.SelectionRepositoryImpl
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun migrationPreservesSelectionAndScheduleAndValidatesSchema() = runBlocking {
        val name = "migration-test.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        val schema = instrumentation.context.assets
            .open("org.exxlexxlee.data.local.AppDatabase/1.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val stored = """{"regionId":"region","regionName":"Region","providerId":"provider","groupId":"1","groupName":"1"}"""
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (index in 0 until setup.length()) db.execSQL(setup.getString(index))
            db.insertOrThrow("selection", null, ContentValues().apply {
                put("id", 1)
                put("json", stored)
            })
            db.execSQL(
                "INSERT INTO schedule_cache (cacheKey, groupId, groupName, todayDate, todayHalfHours, fetchedAtEpochMs) VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf<Any>("region|1", "1", "1", "2026-11-09", List(48) { "ON" }.joinToString(","), 0L),
            )
            db.version = 1
        }
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2).build()
        try {
            val place = database.placeDao().observeFirst().first()!!
            assertEquals(stored, place.json)
            assertEquals(0, place.position)
            val selection = SelectionRepositoryImpl(database.placeDao(), Json).observe().first()!!
            assertEquals("region", selection.region.id)
            val schedule = database.scheduleDao().observe("region|1").first()!!.toDomain()
            assertTrue(schedule.today.published)
            assertEquals(48, schedule.today.halfHours.size)
            assertEquals(false, schedule.schedulesSuspended)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun concurrentSavesAllocateDistinctPositions() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val ids = (1..20).map { async(Dispatchers.IO) { database.placeDao().save("{}", null) } }.awaitAll()
            val places = database.placeDao().observeAll().first()
            assertEquals(20, ids.toSet().size)
            assertEquals((0..19).toList(), places.map { it.position })
        } finally {
            database.close()
        }
    }
}
