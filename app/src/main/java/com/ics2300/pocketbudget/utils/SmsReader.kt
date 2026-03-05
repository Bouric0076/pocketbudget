package com.ics2300.pocketbudget.utils

import android.content.Context
import android.database.Cursor
import android.provider.Telephony

class SmsReader(private val context: Context) {

    data class SmsData(val body: String, val date: Long)

    fun readMpesaMessages(afterTimestamp: Long = 0L): List<SmsData> {
        val messages = mutableListOf<SmsData>()
        val uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE)
        
        // Filter by sender address AND date
        val selection = "(${Telephony.Sms.ADDRESS} LIKE ? OR ${Telephony.Sms.ADDRESS} LIKE ?) AND ${Telephony.Sms.DATE} > ?"
        val selectionArgs = arrayOf("%MPESA%", "%M-PESA%", afterTimestamp.toString()) 
        val sortOrder = "${Telephony.Sms.DATE} ASC" // Read oldest to newest for syncing

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use {
                val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
                
                while (it.moveToNext()) {
                    if (bodyIndex != -1 && dateIndex != -1) {
                        val body = it.getString(bodyIndex)
                        val date = it.getLong(dateIndex)
                        messages.add(SmsData(body, date))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return messages
    }
}
