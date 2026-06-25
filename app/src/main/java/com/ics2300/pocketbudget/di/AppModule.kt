package com.ics2300.pocketbudget.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ics2300.pocketbudget.data.AppDatabase
import com.ics2300.pocketbudget.data.NotificationDao
import com.ics2300.pocketbudget.data.NotificationRepository
import com.ics2300.pocketbudget.data.TransactionDao
import com.ics2300.pocketbudget.data.TransactionRepository
import com.ics2300.pocketbudget.utils.SmsReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `actor_category_mappings` (
                    `partyName` TEXT NOT NULL,
                    `categoryId` INTEGER NOT NULL,
                    PRIMARY KEY(`partyName`)
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `subtype` TEXT NOT NULL DEFAULT 'General'")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `severity` TEXT NOT NULL DEFAULT 'NORMAL'")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `expandedMessage` TEXT")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `amount` REAL")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `currency` TEXT NOT NULL DEFAULT 'KES'")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `categoryLabel` TEXT")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `transactionId` TEXT")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `actorName` TEXT")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `isExpandable` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `originalMessage` TEXT")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `balanceAfter` REAL")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `transactionCost` REAL")
            db.execSQL("ALTER TABLE `notifications` ADD COLUMN `actionData` TEXT")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        net.sqlcipher.database.SQLiteDatabase.loadLibs(context)
        val dbFile = context.getDatabasePath("pocket-budget-database")
        val passphrase = getOrCreateDbPassphrase(context)

        if (dbFile.exists()) {
            encryptExistingDatabaseIfNecessary(context, dbFile, passphrase)
        }

        val factory = net.sqlcipher.database.SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pocket-budget-database"
        )
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
            .build()
    }

    private fun getOrCreateDbPassphrase(context: Context): ByteArray {
        return try {
            val masterKey = androidx.security.crypto.MasterKey.Builder(context.applicationContext)
                .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                .build()

            val securePrefs = androidx.security.crypto.EncryptedSharedPreferences.create(
                context.applicationContext,
                "secure_prefs",
                masterKey,
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val keyName = "db_passphrase_key"
            val storedKey = securePrefs.getString(keyName, null)
            if (storedKey != null) {
                android.util.Base64.decode(storedKey, android.util.Base64.NO_WRAP)
            } else {
                val key = ByteArray(32)
                java.security.SecureRandom().nextBytes(key)
                val encodedKey = android.util.Base64.encodeToString(key, android.util.Base64.NO_WRAP)
                securePrefs.edit().putString(keyName, encodedKey).apply()
                key
            }
        } catch (e: Exception) {
            android.util.Log.e("AppModule", "EncryptedSharedPreferences failed, falling back to static key", e)
            "pocketbudget_fallback_key_2026_06".toByteArray(Charsets.UTF_8)
        }
    }

    private fun encryptExistingDatabaseIfNecessary(
        context: Context,
        dbFile: java.io.File,
        passphrase: ByteArray
    ) {
        var isEncrypted = false
        try {
            val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            )
            db.close()
            isEncrypted = false
        } catch (e: Exception) {
            isEncrypted = true
        }

        if (!isEncrypted) {
            android.util.Log.i("AppModule", "Existing database is unencrypted. Encrypting now...")
            val tempFile = java.io.File(dbFile.parent, dbFile.name + ".tmp")
            if (tempFile.exists()) tempFile.delete()

            try {
                net.sqlcipher.database.SQLiteDatabase.loadLibs(context)

                val unencryptedDb = net.sqlcipher.database.SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    "",
                    null,
                    net.sqlcipher.database.SQLiteDatabase.OPEN_READWRITE
                )

                val hexPassphrase = toHex(passphrase)
                unencryptedDb.rawExecSQL("ATTACH DATABASE '${tempFile.absolutePath}' AS encrypted KEY x'$hexPassphrase'")
                unencryptedDb.rawExecSQL("SELECT sqlcipher_export('encrypted')")
                unencryptedDb.rawExecSQL("DETACH DATABASE encrypted")
                unencryptedDb.close()

                if (dbFile.delete()) {
                    tempFile.renameTo(dbFile)
                    android.util.Log.i("AppModule", "Database successfully encrypted!")
                } else {
                    android.util.Log.e("AppModule", "Failed to delete old unencrypted database.")
                }
            } catch (e: Exception) {
                android.util.Log.e("AppModule", "Encryption migration failed.", e)
            }
        }
    }

    private fun toHex(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF".toCharArray()
        val result = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            result.append(hexChars[i shr 4])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(appDatabase: AppDatabase): TransactionDao {
        return appDatabase.transactionDao()
    }

    @Provides
    @Singleton
    fun provideNotificationDao(appDatabase: AppDatabase): NotificationDao {
        return appDatabase.notificationDao()
    }

    @Provides
    @Singleton
    fun provideSmsReader(@ApplicationContext context: Context): SmsReader {
        return SmsReader(context)
    }

    @Provides
    @Singleton
    fun provideTransactionRepository(
        @ApplicationContext context: Context,
        appDatabase: AppDatabase,
        transactionDao: TransactionDao,
        smsReader: SmsReader
    ): TransactionRepository {
        return TransactionRepository(context, appDatabase, transactionDao, smsReader)
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(
        notificationDao: NotificationDao
    ): NotificationRepository {
        return NotificationRepository(notificationDao)
    }
}