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
package com.pamirs.pradar.scope;


import com.shulie.instrument.simulator.api.scope.AttachmentFactory;
import com.shulie.instrument.simulator.api.scope.InterceptorScopeInvocation;

import static com.shulie.instrument.simulator.api.scope.ExecutionPolicy.*;

/**
 * Created by xiaobin on 2017/1/19.
 */
public class DefaultInterceptorScopeInvocation implements InterceptorScopeInvocation {
    private final String name;
    private Object attachment = null;

    private int depth = 0;
    private int skippedBoundary = 0;
    private Runnable releaseCallback;

    public DefaultInterceptorScopeInvocation(String name, Runnable releaseCallback) {
        this.name = name;
        this.releaseCallback = releaseCallback;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean tryEnter(int point) {
        switch (point) {
            case ALWAYS:
                depth++;
                return true;
            case BOUNDARY:
                if (isActive()) {
                    skippedBoundary++;
                    return false;
                } else {
                    depth++;
                    return true;
                }
            case INTERNAL:
                if (isActive()) {
                    depth++;
                    return true;
                } else {
                    return false;
                }
            default:
                throw new IllegalArgumentException("Unexpected: " + point);
        }
    }

    @Override
    public boolean canLeave(int policy) {
        switch (policy) {
            case ALWAYS:
                return true;
            case BOUNDARY:
                if (skippedBoundary == 0 && depth == 1) {
                    return true;
                } else {
                    skippedBoundary--;
                    return false;
                }
            case INTERNAL:
                return depth > 1;
            default:
                throw new IllegalArgumentException("Unexpected: " + policy);
        }
    }

    @Override
    public void leave(int policy) {
        if (depth == 0) {
            throw new IllegalStateException();
        }

        switch (policy) {
            case ALWAYS:
                break;

            case BOUNDARY:
                if (skippedBoundary != 0 || depth != 1) {
                    throw new IllegalStateException("Cannot leave with BOUNDARY interceptor. depth: " + depth);
                }
                break;

            case INTERNAL:
                if (depth <= 1) {
                    throw new IllegalStateException("Cannot leave with INTERNAL interceptor. depth: " + depth);
                }
                break;

            default:
                throw new IllegalArgumentException("Unexpected: " + policy);
        }

        if (--depth == 0) {
            attachment = null;
            if (releaseCallback != null) {
                releaseCallback.run();
            }
        }
    }


    @Override
    public boolean isActive() {
        return depth > 0;
    }

    @Override
    public Object setAttachment(Object attachment) {
        if (!isActive()) {
            throw new IllegalStateException();
        }

        Object old = this.attachment;
        this.attachment = attachment;
        return old;
    }

    @Override
    public Object getOrCreateAttachment(AttachmentFactory factory) {
        if (!isActive()) {
            throw new IllegalStateException();
        }

        if (attachment == null) {
            attachment = factory.createAttachment();
        }

        return attachment;
    }

    @Override
    public Object getAttachment() {
        if (!isActive()) {
            throw new IllegalStateException();
        }

        return attachment;
    }

    @Override
    public Object removeAttachment() {
        if (!isActive()) {
            throw new IllegalStateException();
        }

        Object old = this.attachment;
        this.attachment = null;
        return old;
    }

    @Override
    public String toString() {
        return "InterceptorScopeInvocation(" + name + ")[depth=" + depth + "]";
    }


}

