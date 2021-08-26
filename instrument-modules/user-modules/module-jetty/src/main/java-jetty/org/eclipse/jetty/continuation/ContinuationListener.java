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
package org.eclipse.jetty.continuation;

import javax.servlet.ServletRequestListener;
import java.util.EventListener;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/12/25 2:15 下午
 */
public interface ContinuationListener extends EventListener
{
    /* ------------------------------------------------------------ */
    /**
     * Called when a continuation life cycle is complete and after
     * any calls to {@link ServletRequestListener#requestDestroyed(javax.servlet.ServletRequestEvent)}
     * The response may still be written to during the call.
     *
     * @param continuation
     */
    public void onComplete(Continuation continuation);

    /* ------------------------------------------------------------ */
    /**
     * Called when a suspended continuation has timed out.
     * The response may be written to and the methods
     * {@link Continuation#resume()} or {@link Continuation#complete()}
     * may be called by a onTimeout implementation,
     * @param continuation
     */
    public void onTimeout(Continuation continuation);

}
