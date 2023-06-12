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
package com.shulie.instrument.simulator.perf;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/1/7 5:51 下午
 */
public final class PerfConstants {
    public final static String MODULE_NAME = "perf";

    /**
     * command 调用模块 thread 的 uniqueId
     */
    public final static String MODULE_ID_THREAD = "thread";
    public final static String MODULE_ID_GC = "gc";
    public final static String MODULE_ID_MEMORY = "memory";
    public final static String MODULE_ID_DUMP = "heapdump";
    public final static String MODULE_ID_TRACE = "trace";
    public final static String MODULE_COMMAND_THREAD_INFO = "info";
    public final static String MODULE_COMMAND_GC_INFO = "info";
    public final static String MODULE_COMMAND_MEMORY_INFO = "info";
    public final static String MODULE_COMMAND_TRACE_INFO = "info";
    public final static String MODULE_COMMAND_DUMP = "dump";

    /**
     * 是否允许业务流量采集明细信息
     */
    public final static String BIZ_REQUEST_ENABLE_PERF_KEY = "pradar.biz.perf.push.enabled";
}
