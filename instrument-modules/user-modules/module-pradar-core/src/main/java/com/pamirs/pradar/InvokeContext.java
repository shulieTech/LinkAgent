/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.pradar;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;

import static com.pamirs.pradar.AppNameUtils.appName;

/**
 * 调用上下文，如果不了解 Pradar 的埋点机制，请勿修改
 * <p>
 * 多个线程之间的传递使用 toMap 转换成 Map,不能直接拿当前对象在多个线程内部传递
 */
public final class InvokeContext extends AbstractContext implements Cloneable {
    public static final String EMPTY = "";
    public static final int INVOKE_ID_LENGTH_LIMIT = 64;
    public static final String START_THREAD_NAME = "st";
    public static final String END_THREAD_NAME = "et";
    private final static Logger LOGGER = LoggerFactory.getLogger(InvokeContext.class);

    static private final TransmittableThreadLocal<InvokeContext> threadLocal
            = new TransmittableThreadLocal<InvokeContext>() {

//        @Override
//        public InvokeContext copy(InvokeContext parentValue) {
//            if (parentValue == null) {
//                parentValue = buildEmptyInvokeContext();
//                parentValue.setLogType(-2);
//            } /*else if (!parentValue.isEmpty() && parentValue.getStartTime() > 1) {
//                long l = System.currentTimeMillis() - parentValue.getStartTime();
//                if (l > 120 * 1000 && parentValue.childInvokeIdx.get() % 100 == 0) {
//                    // 有现场一直是当前线程往当前线程里塞任务，所以如果是当前线程则不打印日志
//                    if (!Thread.currentThread().toString().equals(parentValue.getLocalAttribute("parentThread"))) {
//                        LOGGER.warn(
//                                "[traceDebug] ttl copy trace, but startTime is over {}ms, Thread: {},childRpcId: {}, context:{}",
//                                l, Thread.currentThread(), parentValue.childInvokeIdx.get(), JSON.toJSONString(parentValue));
//                        LOGGER.warn("stack : {}", stackToString());
//                    }
//                }
//            }
//            parentValue.putLocalAttribute("parentThread", Thread.currentThread().getId() + ':' + Thread.currentThread().getName());*/
//            return super.copy(parentValue);
//        }
//
//        @Override
//        protected void beforeExecute() {
//            InvokeContext invokeContext = Pradar.getInvokeContext();
//            if (invokeContext != null && invokeContext.getLogType() == -2) {
//                Pradar.clearInvokeContext();
//            }
//            super.beforeExecute();
//        }

        @Override
        protected void afterExecute() {
            super.afterExecute();
            //线程结束强制回收当前线程上下文
            remove();
        }
    };
    private final static AtomicInteger idx = new AtomicInteger(0);

    /**
     * 当前同级调用的索引号，用来分配调用 ID的
     */
    final private AtomicInteger childInvokeIdx;
    /**
     * 当前上下文的唯一标识
     */
    private final long id;
    /**
     * 父调用上下文
     */
    InvokeContext parentInvokeContext;
    /**
     * 是否需要线程兜底 commit
     */
    private boolean isThreadCommit;

    // log control event ctx
    InvokeContext(int logType) {
        this(EMPTY, Pradar.ROOT_INVOKE_ID, null, StringUtils.EMPTY, StringUtils.EMPTY);
        this.logType = logType;
        initAttribute();
    }

    // root RPC context
    InvokeContext(String _traceId, String _traceAppName, String _invokeId) {
        this(_traceId, _traceAppName, _invokeId, null);
    }

    // when call other service
    InvokeContext(String _traceId, String _traceAppName, String _invokeId, InvokeContext _parentInvokeContext) {
        this(_traceId, _traceAppName, _invokeId, _parentInvokeContext, new AtomicInteger(0));
    }

    // childRpcIdx for clone
    InvokeContext(String _traceId, String _traceAppName, String _invokeId, InvokeContext _parentInvokeContext,
                  AtomicInteger _childRpcIdx) {
        super(_traceId, _traceAppName, _invokeId);
        parentInvokeContext = _parentInvokeContext;
        childInvokeIdx = _childRpcIdx;
        id = idx.incrementAndGet();
        initAttribute();
    }

    static String stackToString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            stringBuilder.append(stackTraceElement.toString()).append("\n\r");
        }
        return stringBuilder.toString();
    }

    // for clone
    InvokeContext(String _traceId, String _traceAppName, String _invokeId, InvokeContext _parentInvokeContext,
                  AtomicInteger _childRpcIdx, long _id) {
        super(_traceId, _traceAppName, _invokeId);
        parentInvokeContext = _parentInvokeContext;
        childInvokeIdx = _childRpcIdx;
        id = _id;
        initAttribute();
    }
    //===============

    // new root RPC context
    InvokeContext(String _traceId, String _traceAppName, String _invokeId, String traceMethod,
                  String traceServiceName) {
        this(_traceId, _traceAppName, _invokeId, null, traceMethod, traceServiceName);
    }

    InvokeContext(String _traceId, String _traceAppName, String _invokeId, InvokeContext _parentInvokeContext
            , String traceMethod, String traceServiceName) {
        this(_traceId, _traceAppName, _invokeId, _parentInvokeContext, new AtomicInteger(0)
                , traceMethod, traceServiceName);
    }

    InvokeContext(String _traceId, String _traceAppName, String _invokeId, InvokeContext _parentInvokeContext,
                  AtomicInteger _childRpcIdx
            , String traceMethod, String traceServiceName) {
        super(_traceId, _traceAppName, _invokeId, traceMethod, traceServiceName);
        parentInvokeContext = _parentInvokeContext;
        childInvokeIdx = _childRpcIdx;
        this.id = idx.incrementAndGet();
        initAttribute();
    }

    /**
     * 创建一个空的上下文
     *
     * @return
     */
    static InvokeContext buildEmptyInvokeContext() {
        return new InvokeContext("empty", "", "", "", "");
    }

    /**
     * InvokeContext backup/restore
     */
    static void set(InvokeContext ctx) {
        threadLocal.set(ctx);
    }

    static InvokeContext get() {
        return threadLocal.get();
    }

    /**
     * 获取被规则处理后的服务名称
     *
     * @param serviceName
     * @return
     */
    private static String getRegularServiceName(String serviceName, String methodName) {
        Set<String> traceRules = GlobalConfig.getInstance().getTraceRules();
        for (String rule : traceRules) {
            if (matches(serviceName, methodName, rule)) {
                return rule;
            }
        }
        return serviceName;
    }

    /**
     * 比较目标是否匹配
     *
     * @param serviceName
     * @param pattern
     * @return
     */
    private static boolean matches(String serviceName, String methodName, String pattern) {
        String patternMethod = "";
        if (pattern != null) {
            final int index = pattern.lastIndexOf('#');
            if (index != -1) {
                patternMethod = pattern.substring(index + 1);
            }
        }
        if (!StringUtils.equalsIgnoreCase(patternMethod, methodName)) {
            return false;
        }
        /**
         * 去除schema 和域名等
         * 这个地方是为了取出 path
         */
        serviceName = toPath(serviceName);
        pattern = toPath(pattern);
        if (StringUtils.indexOf(pattern, '{') == -1) {
            return StringUtils.equals(serviceName, pattern);
        }

        String[] segs = StringUtils.split(serviceName, '/');
        String[] patternSegs = StringUtils.split(pattern, ',');
        if (segs.length != patternSegs.length) {
            return false;
        }
        for (int i = 0, len = patternSegs.length; i < len; i++) {
            /**
             * 如果是这种匹配符则直接跳过不需要匹配
             */
            if (patternSegs[i].indexOf('{') != -1 && patternSegs[i].indexOf('}') != -1) {
                continue;
            } else {
                /**
                 * 否则需要匹配是否相等，如果不相等则直接返回 false
                 */
                if (!StringUtils.equals(patternSegs[i], segs[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    private static byte[] md5(String data) {
        return md5(getBytesUnchecked(data, "UTF-8"));
    }

    private static byte[] md5(byte[] data) {
        return getMd5Digest().digest(data);
    }

    private static MessageDigest getMd5Digest() {
        return getDigest("MD5");
    }

    static byte[] getBytesUnchecked(String string, String charsetName) {
        if (string == null) {
            return null;
        }
        try {
            return string.getBytes(charsetName);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(charsetName, e);
        }
    }

    static MessageDigest getDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 获取到目标的 path
     *
     * @param name
     * @return
     */
    private static String toPath(String name) {
        /**
         * 去除schema 和域名等
         * 这个地方是为了取出 path
         */
        if (StringUtils.startsWith(name, "http://")) {
            name = name.substring(8);
            name = name.substring(name.indexOf('/') + 1);
        } else if (StringUtils.startsWith(name, "https://")) {
            name = name.substring(9);
            name = name.substring(name.indexOf('/') + 1);
        }
        if (name.length() > 0 && name.charAt(0) != '/') {
            name = "/" + name;
        }
        if (name.indexOf('?') != -1) {
            name = name.substring(0, name.indexOf('?'));
        }
        if (name.indexOf('#') != -1) {
            name = name.substring(0, name.indexOf('#'));
        }
        return name;
    }

    /**
     * 反序列化上下文
     *
     * @param map
     * @param parent
     * @return
     */
    static InvokeContext fromMap(Map<String, String> map, InvokeContext parent) {
        String traceId = map.get(PradarService.PRADAR_TRACE_ID_KEY);
        String traceAppName = map.get(PradarService.PRADAR_TRACE_APPNAME_KEY);
        String invokeId = map.get(PradarService.PRADAR_INVOKE_ID_KEY);
        String userData = map.get(PradarService.PRADAR_USER_DATA_KEY);
        String logType = map.get(PradarService.PRADAR_LOG_TYPE_KEY);
        String remoteAppName = map.get(PradarService.PRADAR_REMOTE_APPNAME_KEY);
        String startTime = map.get(PradarService.PRADAR_START_TIME_KEY);
        String remoteIp = map.get(PradarService.PRADAR_REMOTE_IP);
        String upAppName = map.get(PradarService.PRADAR_UPSTREAM_APPNAME_KEY);
        String serviceName = map.get(PradarService.PRADAR_SERVICE_NAME);
        String methodName = map.get(PradarService.PRADAR_METHOD_NAME);
        String middlewareName = map.get(PradarService.PRADAR_MIDDLEWARE_NAME);

/*      //  ！！！！！！！这个不要放开！！！！！！！！！ passcheck不要进行远程传输..........
        boolean passedCheck = Boolean.parseBoolean(map.get(PradarService.PRADAR_WHITE_LIST_CHECK));
*/
        boolean isClusterTest = ClusterTestUtils.isClusterTestRequest(map.get(PradarService.PRADAR_CLUSTER_TEST_KEY));
        if (!isClusterTest) {
            isClusterTest = ClusterTestUtils.isClusterTestRequest(map.get(PradarService.PRADAR_HTTP_CLUSTER_TEST_KEY));
        }
        boolean isDebug = ClusterTestUtils.isDebugRequest(map.get(PradarService.PRADAR_DEBUG_KEY));
        /**
         * 解决rpcId过长的问题，如果上游传下来的rpcId过长,则将rpcId重置为9
         */
        if (StringUtils.isNotBlank(invokeId) && invokeId.length() >= Byte.MAX_VALUE) {
            invokeId = Pradar.MAL_ROOT_INVOKE_ID;
        } else if (invokeId == null) {
            invokeId = Pradar.ROOT_INVOKE_ID;
        }

        if (traceId == null) {
            if (StringUtils.isNotBlank(remoteIp)) {
                traceId = TraceIdGenerator.generate(remoteIp, isClusterTest);
            } else {
                traceId = TraceIdGenerator.generate(isClusterTest);
            }
        }

        InvokeContext ctx = new InvokeContext(traceId, traceAppName, invokeId, parent);
        ctx.importUserData(userData);
        if (null != startTime) {
            ctx.setStartTime(Long.valueOf(startTime));
        }
        if (remoteIp != null) {
            ctx.setRemoteIp(remoteIp);
        }
        if (NumberUtils.isDigits(logType)) {
            ctx.setLogType(Integer.valueOf(logType));
        }
        ctx.setRemoteAppName(remoteAppName);
        ctx.setUpAppName(upAppName);
        ctx.setClusterTest(isClusterTest);
        ctx.setDebug(isDebug);
        /* ctx.setPassCheck(passedCheck);*/
        if (StringUtils.isNotBlank(serviceName)) {
            ctx.setServiceName(serviceName);
        }
        if (StringUtils.isNotBlank(methodName)) {
            ctx.setMethodName(methodName);
        }
        if (StringUtils.isNotBlank(middlewareName)) {
            ctx.setMiddlewareName(middlewareName);
        }
        return ctx;
    }

    /**
     * 从Map中构建InvokeContext
     *
     * @param map
     * @return
     */
    static InvokeContext fromMap(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        return fromMap(map, null);
    }

    private void initAttribute() {
        putLocalAttribute(START_THREAD_NAME, Thread.currentThread().getId() + ":" + Thread.currentThread().getName());
    }

    private void initEndAttribute() {
        putLocalAttribute(END_THREAD_NAME, Thread.currentThread().getId() + ":" + Thread.currentThread().getName());
    }

    public boolean isEmpty() {
        return StringUtils.equals(traceId, "empty");
    }

    String nextChildInvokeId() {
        String childInvokeId = invokeId + "." + childInvokeIdx.incrementAndGet();
        /**
         * 检查rpcId是否超出
         */
        if (checkInvokeIdOverLoad(childInvokeId)) {
            return Pradar.ADJUST_ROOT_INVOKE_ID;
        }
        return childInvokeId;
    }

    /**
     * 判断是否是顶点入口
     *
     * @return
     */
    public boolean isRoot() {
        return StringUtils.equals(invokeId, Pradar.ROOT_INVOKE_ID) || StringUtils.equals(invokeId,
                Pradar.MAL_ROOT_INVOKE_ID);
    }

    @Override
    public void setMiddlewareName(String middlewareName) {
        super.setMiddlewareName(middlewareName);
        /**
         * 判断并且为空是为了防止 invokeId 过长强行设置为 9导致 traceNode 会被重新设置
         */
        if (isRoot()) {
            removeUserData(PradarService.PRADAR_TRACE_NODE_KEY);
            setTraceNode(generateTraceNode());
        }

    }

    protected InvokeContext cloneInstance() {
        InvokeContext clone = new InvokeContext(traceId, traceAppName, getInvokeId(), parentInvokeContext,
                childInvokeIdx);
        clone.attributes = this.attributes;
        clone.localAttributes = this.localAttributes;

        clone.traceName = this.traceName;
        clone.serviceName = this.serviceName;
        clone.methodName = this.methodName;
        clone.remoteIp = this.remoteIp;
        clone.callBackMsg = this.callBackMsg;
        clone.logType = this.logType;
        clone.invokeType = this.invokeType;
        clone.middlewareName = this.middlewareName;
        clone.resultCode = this.resultCode;
        clone.startTime = this.startTime;
        clone.logTime = this.logTime;
        clone.requestSize = this.requestSize;
        clone.responseSize = this.responseSize;
        clone.request = this.request;
        clone.response = this.response;
        clone.isClusterTest = this.isClusterTest();
        clone.isDebug = this.isDebug();
        return clone;
    }

    /**
     * 创建子 RPC 上下文
     */
    public InvokeContext createChildInvoke() {
        final InvokeContext parent;
        if (checkInvokeIdOverLoad(this.invokeId) && this.parentInvokeContext != null
                && this.parentInvokeContext.parentInvokeContext != null) {
            // 当前 InvokeContext 创建子 InvokeContext，一般当前 Context 就是服务端或者入口端，
            // 正常情况不应该再有 parent。如果 invokeId 过长，而且又存在 parent，
            // parent->parent，很可能就是埋点出现问题，比如一直 startInvoke，没有 endInvoke，
            // 会导致 InvokeContext 嵌套过深的内存泄漏。这个时候重新创建一个上下文，使上面的上下文都能够释放。
            LOGGER.warn("InvokeContext leak detected, traceId={}, invokeId={}", traceId, invokeId);
            parent = new InvokeContext(traceId, traceAppName, Pradar.ADJUST_ROOT_INVOKE_ID, traceMethod,
                    traceServiceName);
        } else {
            parent = this;
        }
        InvokeContext ctx = new InvokeContext(traceId, traceAppName, nextChildInvokeId(), parent);
        ctx.attributes = this.attributes;
        ctx.setClusterTest(this.isClusterTest());
        ctx.setDebug(this.isDebug());
        return ctx;
    }

    /**
     * check invokeId is overload
     */
    public boolean checkInvokeIdOverLoad(String invokeId) {
        if (StringUtils.isBlank(invokeId)) {
            return false;
        }
        return invokeId.length() > INVOKE_ID_LENGTH_LIMIT;
    }

    /**
     * 获取上一层调用上下文
     */
    public InvokeContext getParentInvokeContext() {
        return parentInvokeContext;
    }

    public void setParentInvokeContext(InvokeContext invokeContext) {
        this.parentInvokeContext = invokeContext;
    }

    /**
     * 外置的 Pradar 埋点逻辑，方便在不同的中间件做埋点
     */
    public void startTrace(String serviceName, String methodName) {
        this.logType = Pradar.LOG_TYPE_TRACE;
        this.startTime = System.currentTimeMillis();
        this.serviceName = serviceName;
        this.methodName = methodName;
        /**
         * 生成入口节点的唯一标识
         */
        String traceNode = generateNodeId(null, serviceName, methodName, middlewareName);
        /**
         * 生成当前节点的唯一标识
         */
        setTraceNode(traceNode);
        setNodeId(traceNode);
    }

    public void endTrace(String result, int type) {
        endTrace(result, type, null);
    }

    public void endTrace(String result, int type, String appendMsg) {
        if (this.logType != Pradar.LOG_TYPE_TRACE) {
            LOGGER.error("context mismatch at endTrace(), logType={}, middleware={}, currentMiddlewareType: {}",
                    this.logType, this.middlewareName, type);
            this.logType = Pradar.LOG_TYPE_EVENT_ILLEGAL;
            return;
        }
        setLogTime(System.currentTimeMillis());
        this.resultCode = result;
        this.invokeType = type;
        if (appendMsg != null) {
            this.callBackMsg = appendMsg;
        }
        initEndAttribute();
    }

    /**
     * 客户端调用的开始
     */
    public void startClientInvoke(String serviceName, String methodName) {
        this.logType = Pradar.LOG_TYPE_INVOKE_CLIENT;
        this.startTime = System.currentTimeMillis();
        this.serviceName = serviceName;
        this.methodName = methodName;
        setNodeId(generateNodeId());
    }

    /**
     * 客户端调用的结束
     *
     * @param result
     * @param type
     */
    public void endClientInvoke(String result, int type) {
        if (this.logType != Pradar.LOG_TYPE_INVOKE_CLIENT) {
            LOGGER.warn("context mismatch at endRpc(), logType={}", this.logType);
            this.logType = Pradar.LOG_TYPE_EVENT_ILLEGAL;
            return;
        }
        setLogTime(System.currentTimeMillis());
        this.invokeType = type;
        this.resultCode = result;
        initEndAttribute();
    }

    /**
     * 服务端调用开始
     *
     * @param serviceName
     * @param methodName
     */
    public void startServerInvoke(String serviceName, String methodName) {
        this.logType = Pradar.LOG_TYPE_INVOKE_SERVER;
        this.startTime = System.currentTimeMillis();
        this.serviceName = serviceName;
        this.methodName = methodName;
        /**
         * 生成当前节点的唯一标识
         */
        setNodeId(generateNodeId());
    }

    /**
     * 服务端调用结束
     *
     * @deprecated 使用 {@link #endServerInvoke(int, String)}
     */
    public void endServerInvoke(int type) {
        endServerInvoke(type, null);
    }

    /**
     * 服务端调用结束
     *
     * @param type
     * @param result
     */
    public void endServerInvoke(int type, String result) {
        if (this.logType != Pradar.LOG_TYPE_INVOKE_SERVER) {
            if (!ResultCode.INVOKE_RESULT_UNKNOWN.equals(result)) {
                LOGGER.warn("context mismatch at rpcServerSend(), logType={}", this.logType);
            }
            this.logType = Pradar.LOG_TYPE_EVENT_ILLEGAL;
            return;
        }
        setLogTime(System.currentTimeMillis());
        this.resultCode = result;
        if (type >= 0) {
            this.invokeType = type;
        }
        initEndAttribute();
    }

    public void endServerInvoke(String result) {
        endServerInvoke(-1, result);
    }

    /**
     * 为了可以更快的让内存回收
     */
    @Override
    public void destroy() {
        super.destroy();
    }

    public String getTraceNode() {
        return getUserData(PradarService.PRADAR_TRACE_NODE_KEY);
    }

    public void setTraceNode(String traceNode) {
        putUserData(PradarService.PRADAR_TRACE_NODE_KEY, traceNode);
    }

    public String getNodeId() {
        return getLocalAttribute(PradarService.PRADAR_NODE_ID_KEY);
    }

    public void setNodeId(String nodeId) {
        putLocalAttribute(PradarService.PRADAR_NODE_ID_KEY, nodeId);
    }

    /**
     * 把 InvokeContext 导出为 Map 进行传输，以便网络传输时序列化可以兼容新老版本。
     */
    public Map<String, String> toMap() {
        Map<String, String> context = new HashMap<String, String>();
        if (StringUtils.isNotBlank(traceId)) {
            context.put(PradarService.PRADAR_TRACE_ID_KEY, traceId);
        }
        context.put(PradarService.PRADAR_TRACE_APPNAME_KEY, traceAppName == null ? appName() : traceAppName);
        if (StringUtils.isNotBlank(invokeId)) {
            context.put(PradarService.PRADAR_INVOKE_ID_KEY, invokeId);
        }
        String userData = exportUserData();
        if (StringUtils.isNotBlank(userData)) {
            context.put(PradarService.PRADAR_USER_DATA_KEY, exportUserData());
        }
        context.put(PradarService.PRADAR_REMOTE_APPNAME_KEY, appName());
        context.put(PradarService.PRADAR_LOG_TYPE_KEY, String.valueOf(logType));
        context.put(PradarService.PRADAR_START_TIME_KEY, String.valueOf(startTime));
        if (StringUtils.isNotBlank(remoteIp)) {
            context.put(PradarService.PRADAR_REMOTE_IP, remoteIp);
        }
        context.put(PradarService.PRADAR_UPSTREAM_APPNAME_KEY, upAppName == null ? appName() : upAppName);
        context.put(PradarService.PRADAR_CLUSTER_TEST_KEY,
                isClusterTest() ? Pradar.PRADAR_CLUSTER_TEST_ON : Pradar.PRADAR_CLUSTER_TEST_OFF);
        context.put(PradarService.PRADAR_DEBUG_KEY, isDebug() ? Pradar.PRADAR_DEBUG_ON : Pradar.PRADAR_DEBUG_OFF);
/*
        context.put(PradarService.PRADAR_WHITE_LIST_CHECK, String.valueOf(isPassCheck()));
*/
        if (serviceName != null) {
            context.put(PradarService.PRADAR_SERVICE_NAME, serviceName);
        }
        if (methodName != null) {
            context.put(PradarService.PRADAR_METHOD_NAME, methodName);
        }
        if (middlewareName != null) {
            context.put(PradarService.PRADAR_MIDDLEWARE_NAME, middlewareName);
        }
        return context;
    }

    protected String generateNodeId(String traceNode, String serviceName, String methodName, String middlewareName) {
        if (StringUtils.startsWith(serviceName, "http://") || StringUtils.startsWith(serviceName, "https://")) {
            return hash(
                    (traceNode == null ? "" : traceNode + '-') + getRegularServiceName(defaultBlankIfNull(serviceName),
                            methodName)

                            + '-' + defaultBlankIfNull(methodName) + '-' + defaultBlankIfNull(middlewareName));
        } else {
            return hash((traceNode == null ? "" : traceNode + '-') + defaultBlankIfNull(serviceName)
                    + '-' + defaultBlankIfNull(methodName) + '-' + defaultBlankIfNull(middlewareName));
        }
    }

    private static String hash(String data) {
        CRC32 crc32 = new CRC32();
        byte[] bytes = data.getBytes();
        crc32.update(bytes, 0, bytes.length);
        return Long.toHexString(crc32.getValue());
    }

    protected String generateNodeId() {
        return "empty";
    }

    protected String generateTraceNode() {
        if (StringUtils.startsWith(serviceName, "http://") || StringUtils.startsWith(serviceName, "https://")) {
            return hash((getTraceNode() == null ? "" : getTraceNode() + '-') + getRegularServiceName(
                    defaultBlankIfNull(serviceName), methodName)
                    + '-' + defaultBlankIfNull(methodName) + '-' + defaultBlankIfNull(middlewareName));
        } else if (middlewareName != null && middlewareName.equals("redis")) {
            return hash((getTraceNode() == null ? "" : getTraceNode() + '-') + defaultBlankIfNull(serviceName)
                    + '-' + defaultBlankIfNull(middlewareName));
        } else {
            return hash((getTraceNode() == null ? "" : getTraceNode() + '-') + defaultBlankIfNull(serviceName)
                    + '-' + defaultBlankIfNull(methodName) + '-' + defaultBlankIfNull(middlewareName));
        }
    }

    public boolean isThreadCommit() {
        return isThreadCommit;
    }

    public void setThreadCommit(boolean threadCommit) {
        isThreadCommit = threadCommit;
    }

    private String defaultBlankIfNull(String str) {
        return str == null ? "" : str;
    }

    public long getId() {
        return id;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        InvokeContext temp = null;
        // 不传递父上下文，减少内存开销
        // 传递之后如果开启span， clone 的这个本身就是父上下文，  如果不开启span， 本身这个上下文也没用。
        InvokeContext invokeContext = new InvokeContext(this.traceId, this.traceAppName, this.invokeId, temp,
                new AtomicInteger(this.childInvokeIdx.get()), this.id);

        invokeContext.middlewareName = this.middlewareName;
        invokeContext.isThreadCommit = this.isThreadCommit;
        invokeContext.attributes = new LinkedHashMap<String, String>();
        if (this.attributes != null) {
            invokeContext.attributes.putAll(this.attributes);
        }
        invokeContext.localAttributes = new LinkedHashMap<String, String>();
        if (this.localAttributes != null) {
            invokeContext.localAttributes.putAll(this.localAttributes);
        }
        invokeContext.serviceName = this.serviceName;
        invokeContext.methodName = this.methodName;
        invokeContext.remoteIp = this.remoteIp;
        invokeContext.callBackMsg = this.callBackMsg;
        invokeContext.logType = this.logType;
        invokeContext.upAppName = this.upAppName;
        invokeContext.request = this.request;
        invokeContext.requestSize = this.requestSize;
        invokeContext.response = this.response;
        invokeContext.responseSize = this.responseSize;
        invokeContext.startTime = this.startTime;
        invokeContext.hasError = this.hasError;
        invokeContext.invokeType = this.invokeType;
        invokeContext.isClusterTest = this.isClusterTest;
        invokeContext.isDebug = this.isDebug;
        invokeContext.logTime = this.logTime;
        invokeContext.passCheck = this.passCheck;
        invokeContext.port = this.port;
        invokeContext.resultCode = this.resultCode;
        invokeContext.traceMethod = this.traceMethod;
        invokeContext.traceServiceName = this.traceServiceName;
        invokeContext.remoteAppName = this.remoteAppName;
        invokeContext.traceName = this.traceName;
        return invokeContext;
    }
}
