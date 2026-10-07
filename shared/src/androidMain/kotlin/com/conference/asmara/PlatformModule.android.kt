package com.conference.asmara

import android.content.Context
import com.conference.asmara.data.local.DriverFactory
import com.conference.asmara.push.FirebasePushRegistrar
import com.conference.asmara.push.NoOpPushRegistrar
import com.conference.asmara.push.PushConfig
import com.conference.asmara.push.PushPlatform
import com.conference.asmara.push.PushRegistrar
import org.koin.core.module.Module
import org.koin.dsl.module

fun androidPlatformModule(context: Context): Module = module {
    single { DriverFactory(context) }
    single { PushPlatform.ANDROID }
    // The one place PushConfig is read. Without google-services.json there is
    // no default FirebaseApp, and FirebaseMessaging.getInstance() would throw —
    // so the Firebase registrar must never be constructed in that build.
    single<PushRegistrar> { if (PushConfig.ENABLED) FirebasePushRegistrar(context) else NoOpPushRegistrar }
}
