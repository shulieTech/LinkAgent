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
package com.pamirs.pradar;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;

public class TraceIdGenerator {

    private static String IP_16 = "ffffffff";
    private static String IP_int = "255255255255";
    private static String PID = "0000";
    private static char PID_FLAG = 'd';

    private static AtomicInteger count = new AtomicInteger(1000);

    static {
        try {
            String ipAddress = PradarCoreUtils.getLocalAddress();
            if (ipAddress != null) {
                IP_16 = getIP_16(ipAddress);
                IP_int = getIP_int(ipAddress);
            }

            PID = getHexPid(getPid());
        } catch (Throwable e) {
        }
    }

    static String getHexPid(int pid) {
        // unsign short 0 to 65535
        if (pid < 0) {
            pid = 0;
        }
        if (pid > 65535) {
            String strPid = Integer.toString(pid);
            strPid = strPid.substring(strPid.length() - 4, strPid.length());
            pid = Integer.parseInt(strPid);
        }
        String str = Integer.toHexString(pid);
        while (str.length() < 4) {
            str = "0" + str;
        }
        return str;
    }

    /**
     * get current pid,max pid 32 bit systems 32768, for 64 bit 4194304
     * http://unix.stackexchange.com/questions/16883/what-is-the-maximum-value-of-the-pid-of-a-process
     * <p>
     * http://stackoverflow.com/questions/35842/how-can-a-java-program-get-its-own-process-id
     *
     * @return
     */
    static int getPid() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String name = runtime.getName();
        int pid;
        try {
            pid = Integer.parseInt(name.substring(0, name.indexOf('@')));
        } catch (Exception e) {
            pid = 0;
        }
        return pid;
    }

    private static String getTraceId(String ip, long timestamp, int nextId, boolean isClusterTestRequest) {
        StringBuilder appender = new StringBuilder(32);
        appender.append(ip).append(timestamp).append(getNextIdStr(nextId)).append(PID_FLAG).append(PID);
        if (isClusterTestRequest) {
            appender.append(dealSamplingInterval(PradarSwitcher.getClusterTestSamplingInterval()));
        } else {
            appender.append(dealSamplingInterval(PradarSwitcher.getSamplingInterval()));
        }
        return appender.toString();
    }

    /**
     * 处理采用率，输出4位字符串。
     *
     * @param samplingInterval 采样率
     * @return 4位字符串
     */
    private static String dealSamplingInterval(int samplingInterval) {
        if (samplingInterval < 0 || samplingInterval > 10000) {
            return "0000";
        }
        return String.format("%04d", samplingInterval);
    }

    /**
     * 生成 traceId
     * 需要指定是否是压测流量，因为压测流量采样率与业务流量采样率是隔离开来的
     * 所以生成 traceId 的逻辑会有差异
     *
     * @param isClusterTestRequest 是否是压测流量
     * @return
     */
    public static String generate(boolean isClusterTestRequest) {
        return getTraceId(IP_16, System.currentTimeMillis(), getNextId(isClusterTestRequest), isClusterTestRequest);
    }

    /**
     * 指定 ip生成 traceId
     * 需要指定是否是压测流量，因为压测流量采样率与业务流量采样率是隔离开来的
     * 所以生成 traceId 的逻辑会有差异
     *
     * @param ip                   ip
     * @param isClusterTestRequest 是否是压测流量
     * @return
     */
    public static String generate(String ip, boolean isClusterTestRequest) {
        if (StringUtils.isNotBlank(ip) && validate(ip)) {
            try {
                return getTraceId(getIP_16(ip), System.currentTimeMillis(), getNextId(isClusterTestRequest),
                    isClusterTestRequest);
            } catch (Throwable t) {
                //说明ip格式有问题
                return generate(isClusterTestRequest);
            }
        } else {
            return generate(isClusterTestRequest);
        }
    }

    /**
     * 获取当机机器ip数字型字符串，如127.0.0.1则返回1921681100
     *
     * @return
     */
    public static String generateIpv4Id() {
        return IP_int;
    }

    /**
     * 指定 ip 获取数字型字符串，如127.0.0.1则返回1921681100
     *
     * @param ip
     * @return
     */
    public static String generateIpv4Id(String ip) {
        if (StringUtils.isNotBlank(ip) && validate(ip)) {
            return getIP_int(ip);
        } else {
            return IP_int;
        }
    }

    private static boolean validate(String ip) {
        return StringUtils.countMatches(ip, ".") == 3;
    }

    private static String getIP_16(String ip) {
        String[] ips = StringUtils.split(ip, '.');
        StringBuilder sb = new StringBuilder();
        for (int i = ips.length - 1; i >= 0; --i) {
            String hex = Integer.toHexString(Integer.parseInt(ips[i]));
            if (hex.length() == 1) {
                sb.append('0').append(hex);
            } else {
                sb.append(hex);
            }

        }
        return sb.toString();
    }

    private static String getIP_int(String ip) {
        return ip.replace(".", "");
    }

    private static String getNextIdStr(int nextId) {
        if (nextId > 1000) {
            return String.valueOf(nextId);
        }
        if (nextId > 100) {
            return "0" + nextId;
        }
        if (nextId > 10) {
            return "00" + nextId;
        }
        return "000" + nextId;
    }

    /**
     * 获取 traceId 的数字组成部分，此部分会拿来判断是否进行采样
     * 此值是一个在[1-上限]之内滚动的数字，上限的计算方式是取在10000以内(不包含10000)最大的采样率值的倍数值
     * 如5000则为5000，4999则为9998，依此类推
     * <p>
     * 业务流量与压测流量的采样率不一样，所以针对两种流量的 nextId 的上限也会不一样
     *
     * @param isClusterTestRequest
     * @return
     */
    private static int getNextId(boolean isClusterTestRequest) {
        int si = 0;
        if (isClusterTestRequest) {
            si = PradarSwitcher.getClusterTestSamplingInterval();
        } else {
            si = PradarSwitcher.getSamplingInterval();
        }
        int maxBoundary = 10000 - (9999 % si + 1);

        for (; ; ) {
            int current = count.get();
            int next = (current > maxBoundary) ? 1 : current + 1;
            if (count.compareAndSet(current, next)) {
                return next;
            }
        }
    }

    /**
     * 从 traceId 中获取 nextId
     *
     * @param traceId
     * @return
     */
    public static int getNextId(String traceId) {
        if (traceId.length() < 25) {
            return -1;
        }
        int count = traceId.charAt(21) - '0';
        count = count * 10 + traceId.charAt(22) - '0';
        count = count * 10 + traceId.charAt(23) - '0';
        count = count * 10 + traceId.charAt(24) - '0';
        return count;
    }
}
