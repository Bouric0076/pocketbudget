package com.ics2300.pocketbudget.utils

import android.content.Context
import android.provider.Telephony
import android.util.Log

class SmsReader(
    private val context: Context
) {

    data class SmsData(
        val body: String,
        val date: Long
    )

    fun readMpesaMessages(afterTimestamp: Long = 0L): List<SmsData> {
        val messages = mutableListOf<SmsData>()

        val projection = arrayOf(
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val selection = """
            (${Telephony.Sms.ADDRESS} LIKE ? OR ${Telephony.Sms.ADDRESS} LIKE ?)
            AND ${Telephony.Sms.DATE} > ?
        """.trimIndent()

        val selectionArgs = arrayOf(
            "%MPESA%",
            "%M-PESA%",
            afterTimestamp.toString()
        )

        val sortOrder = "${Telephony.Sms.DATE} ASC"

        return try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->

                val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (cursor.moveToNext()) {
                    val body = cursor.getString(bodyIndex).orEmpty()
                    val date = cursor.getLong(dateIndex)

                    if (body.isNotBlank() && date > 0L) {
                        messages.add(SmsData(body, date))
                    }
                }
            }

            messages
        } catch (e: SecurityException) {
            Log.e(TAG, "SMS permission denied while reading M-Pesa messages.", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read M-Pesa SMS messages.", e)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "SmsReader"
    }
}