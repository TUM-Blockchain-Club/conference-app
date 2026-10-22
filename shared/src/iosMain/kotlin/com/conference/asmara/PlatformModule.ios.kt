package com.conference.asmara

import com.conference.asmara.data.local.DriverFactory
import com.conference.asmara.push.ApnsPushRegistrar
import com.conference.asmara.push.NoOpPushRegistrar
import com.conference.asmara.push.PushConfig
import com.conference.asmara.push.PushPlatform
import com.conference.asmara.push.PushRegistrar
import org.koin.core.module.Module
import org.koin.dsl.module

fun iosPlatformModule(): Module = module {
    single { DriverFactory() }
    single { PushPlatform.IOS }
    // The one place PushConfig is read. Without GoogleService-Info.plist the
    // app never calls registerForRemoteNotifications, so there is no permission
    // prompt and no token — which is also all a simulator could do anyway.
    single<PushRegistrar> { if (PushConfig.ENABLED) ApnsPushRegistrar() else NoOpPushRegistrar }
}
