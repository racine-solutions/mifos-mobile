/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package cmp.android.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import org.mifos.mobile.core.ui.utils.SmsCodeReader
import org.mifos.mobile.core.ui.utils.extractSixDigitCode

/** SMS Retriever based reader: needs no SMS permission, only the app hash at the end of the SMS. */
class AndroidSmsCodeReader(private val context: Context) : SmsCodeReader {

    override fun start(onCode: (String) -> Unit): () -> Unit {
        val appContext = context.applicationContext
        var registered = false

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != SmsRetriever.SMS_RETRIEVED_ACTION) return
                val status = intent.extras?.get(SmsRetriever.EXTRA_STATUS) as? Status ?: return
                if (status.statusCode == CommonStatusCodes.SUCCESS) {
                    val message = intent.extras?.getString(SmsRetriever.EXTRA_SMS_MESSAGE).orEmpty()
                    extractSixDigitCode(message)?.let(onCode)
                }
                // A timeout or any other status just leaves the member to type the code.
            }
        }

        val stop = {
            if (registered) {
                registered = false
                runCatching { appContext.unregisterReceiver(receiver) }
            }
        }

        runCatching {
            val filter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(
                    receiver,
                    filter,
                    SmsRetriever.SEND_PERMISSION,
                    null,
                    Context.RECEIVER_EXPORTED,
                )
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                appContext.registerReceiver(receiver, filter, SmsRetriever.SEND_PERMISSION, null)
            }
            registered = true
            SmsRetriever.getClient(appContext).startSmsRetriever()
                .addOnFailureListener { stop() }
        }.onFailure { stop() }

        return stop
    }
}
