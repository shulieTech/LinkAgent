/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.akka;

import com.pamirs.attach.plugin.akka.interceptor.MailboxDequeueInterceptor;
import com.pamirs.attach.plugin.akka.interceptor.MailboxEnqueueInterceptor;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;

/**
 * @author xiaobin.zfb
 * @since 2020/8/13 4:10 下午
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = AkkaConstants.PLUGIN_NAME, version = "1.0.0", author = "xiaobin@shulie.io", description = "akka 并发框架")
public class AkkaPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {
        this.enhanceTemplate.enhanceWithSuperClass(this, "akka.dispatch.Mailbox", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod enqueueMethod = target.getDeclaredMethod("enqueue", "akka.actor.ActorRef", "akka.dispatch.Envelope");
                enqueueMethod.addInterceptor(Listeners.of(MailboxEnqueueInterceptor.class));

                InstrumentMethod dequeueMethod = target.getDeclaredMethod("dequeue");
                dequeueMethod.addInterceptor(Listeners.of(MailboxDequeueInterceptor.class));
            }
        });

        this.enhanceTemplate.enhance(this, "akka.dispatch.Mailbox", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod enqueueMethod = target.getDeclaredMethod("enqueue", "akka.actor.ActorRef", "akka.dispatch.Envelope");
                enqueueMethod.addInterceptor(Listeners.of(MailboxEnqueueInterceptor.class));

                InstrumentMethod dequeueMethod = target.getDeclaredMethod("dequeue");
                dequeueMethod.addInterceptor(Listeners.of(MailboxDequeueInterceptor.class));
            }
        });
        return true;
    }

}
