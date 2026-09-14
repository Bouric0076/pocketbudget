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

    private const val LEGACY_FALLBACK_DB_KEY = "pocketbudget_fallback_key_2026_06"

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

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `cashFlowBucket` TEXT NOT NULL DEFAULT 'EXPENSE'")
            db.execSQL(
                """
                UPDATE transactions
                SET cashFlowBucket = CASE
                    WHEN categoryId IN (SELECT id FROM categories WHERE name = 'Savings') THEN 'SAVINGS'
                    WHEN categoryId IN (SELECT id FROM categories WHERE name = 'Transfer & Cash') THEN 'TRANSFER'
                    WHEN type IN ('Received', 'Deposit') THEN 'INCOME'
                    ELSE 'EXPENSE'
                END
                """.trimIndent()
            )
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        net.sqlcipher.database.SQLiteDatabase.loadLibs(context)
        val dbFile = context.getDatabasePath("pocket-budget-database")
        val passphrase = resolveDbPassphrase(
            dbFile,
            getOrCreateDbPassphrase(context)
        )

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
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
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
            android.util.Log.e("AppModule", "Unable to access encrypted database key storage", e)
            throw IllegalStateException(
                "Secure database key storage is unavailable; refusing to use an insecure fallback.",
                e
            )
        }
    }

    /**
     * Older builds could encrypt the database with the fallback key when
     * EncryptedSharedPreferences was unavailable. Try that key only when an
     * existing encrypted database proves that the current key is not usable.
     * This keeps existing user data recoverable during the security migration.
     */
    private fun resolveDbPassphrase(
        dbFile: java.io.File,
        currentPassphrase: ByteArray
    ): ByteArray {
        if (!dbFile.exists()) return currentPassphrase
        if (canOpenEncryptedDatabase(dbFile, currentPassphrase)) return currentPassphrase

        val legacyPassphrase = LEGACY_FALLBACK_DB_KEY.toByteArray(Charsets.UTF_8)
        if (canOpenEncryptedDatabase(dbFile, legacyPassphrase)) {
            android.util.Log.w(
                "AppModule",
                "Recovered database with the legacy SQLCipher key; retaining it for compatibility."
            )
            return legacyPassphrase
        }

        return currentPassphrase
    }

    private fun canOpenEncryptedDatabase(
        dbFile: java.io.File,
        passphrase: ByteArray
    ): Boolean {
        return try {
            val db = net.sqlcipher.database.SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                passphrase,
                null,
                net.sqlcipher.database.SQLiteDatabase.OPEN_READONLY,
                null,
                null
            )
            try {
                val cursor = db.rawQuery(
                    "SELECT count(*) FROM sqlite_master",
                    emptyArray<String>()
                )
                try {
                    cursor.moveToFirst()
                } finally {
                    cursor.close()
                }
            } finally {
                db.close()
            }
            true
        } catch (_: Exception) {
            false
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

                // Create the encrypted destination first, then attach the
                // plaintext source. This avoids relying on ATTACH to create
                // and initialize a SQLCipher database on Android.
                val encryptedDb = net.sqlcipher.database.SQLiteDatabase.openDatabase(
                    tempFile.absolutePath,
                    passphrase,
                    null,
                    net.sqlcipher.database.SQLiteDatabase.OPEN_READWRITE or
                        net.sqlcipher.database.SQLiteDatabase.CREATE_IF_NECESSARY,
                    null,
                    null
                )

                try {
                    val sourcePath = dbFile.absolutePath.replace("'", "''")
                    encryptedDb.rawExecSQL("ATTACH DATABASE '$sourcePath' AS plain KEY ''")
                    val exportCursor = encryptedDb.rawQuery(
                        "SELECT sqlcipher_export('main', 'plain')",
                        emptyArray<String>()
                    )
                    try {
                        exportCursor.moveToFirst()
                    } finally {
                        exportCursor.close()
                    }
                    encryptedDb.rawExecSQL("DETACH DATABASE plain")
                } finally {
                    encryptedDb.close()
                }

                val backupFile = java.io.File(dbFile.parent, dbFile.name + ".unencrypted-backup")
                if (backupFile.exists()) backupFile.delete()

                if (!dbFile.renameTo(backupFile)) {
                    throw java.io.IOException("Could not stage the existing database for encryption")
                }

                if (!tempFile.renameTo(dbFile)) {
                    backupFile.renameTo(dbFile)
                    throw java.io.IOException("Could not install the encrypted database")
                }

                backupFile.delete()
                android.util.Log.i("AppModule", "Database successfully encrypted!")
            } catch (e: Exception) {
                android.util.Log.e("AppModule", "Encryption migration failed.", e)
            }
        }
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
