package com.pamirs.pradar.script.bsh;

import bsh.EvalError;
import bsh.Interpreter;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.internal.config.MockConfig;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.MockConfigAddEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.MockConfigModifyEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.MockConfigRemoveEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.script.ScriptEvaluator;
import com.pamirs.pradar.script.commons.InterpreterWrapper;

import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GlobalCacheBshScriptEvaluator implements ScriptEvaluator {

    private ClassLoader classLoader;

    private Map<String, InterpreterWrapper[]> interpreterCaches = new HashMap<String, InterpreterWrapper[]>();

    private int interceptorCacheNum = Integer.parseInt(System.getProperty("simulator.mock.interceptor.caches.num", "256"));

    /**
     * Construct a new BshScriptEvaluator.
     */
    public GlobalCacheBshScriptEvaluator() {

        EventRouter.router().addListener(new PradarEventListener() {
            @Override
            public EventResult onEvent(IEvent event) {
                synchronized (this) {
                    if (event instanceof MockConfigAddEvent) {
                        for (MockConfig mockConfig : ((MockConfigAddEvent) event).getTarget()) {
                            initScriptCache(mockConfig.getCodeScript());
                        }
                    } else if (event instanceof MockConfigRemoveEvent) {
                        for (MockConfig mockConfig : ((MockConfigAddEvent) event).getTarget()) {
                            interpreterCaches.remove(mockConfig.getCodeScript());
                        }
                    } else if (event instanceof MockConfigModifyEvent) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {

                        }

                        Set<String> scripts = new HashSet<String>();
                        for (MockConfig config : GlobalConfig.getInstance().getMockConfigs()) {
                            scripts.add(config.getCodeScript());
                        }
                        for (String script : scripts) {
                            if (!interpreterCaches.containsKey(script)) {
                                initScriptCache(script);
                            }
                        }

                        Set<String> removed = new HashSet<String>();
                        for (String key : interpreterCaches.keySet()) {
                            if (!scripts.contains(key)) {
                                removed.add(key);
                            }
                        }
                        for (String s : removed) {
                            interpreterCaches.remove(s);
                        }


                    } else {
                        return EventResult.IGNORE;
                    }
                    return EventResult.success("Mock config update successful.");
                }
            }

            @Override
            public int order() {
                return 7;
            }
        });
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public String getType() {
        return "bsh";
    }

    @Override
    public Object evaluate(String script) {
        return evaluate(script, null);
    }

    @Override
    public Object evaluate(String script, Map<String, Object> arguments) {
        InterpreterWrapper wrapper = fetchInterpreterWrapper(script);
        Interpreter interpreter = wrapper.getInterpreter();
        try {
            interpreter.setClassLoader(this.classLoader);
            if (arguments != null) {
                for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                    interpreter.set(entry.getKey(), entry.getValue());
                }
            }
            return interpreter.eval(new StringReader(script));
        } catch (EvalError ex) {
            throw new RuntimeException(script, ex);
        } finally {
            wrapper.setHold(false);
        }
    }

    @Override
    public Object evaluate(ClassLoader classLoader, String script, Map<String, Object> arguments) {
        InterpreterWrapper wrapper = fetchInterpreterWrapper(script);
        Interpreter interpreter = wrapper.getInterpreter();
        try {
            // 非必要不刷新ClassLoader
            ClassLoader externalClassLoader = ReflectionUtils.get(interpreter.getClassManager(), "externalClassLoader");
            if (externalClassLoader != classLoader) {
                interpreter.setClassLoader(classLoader);
            }
            if (arguments != null) {
                for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                    interpreter.set(entry.getKey(), entry.getValue());
                }
            }
            return interpreter.eval(new StringReader(script));
        } catch (EvalError ex) {
            throw new RuntimeException(script, ex);
        } finally {
            wrapper.setHold(false);
        }
    }

    private InterpreterWrapper fetchInterpreterWrapper(String script) {
        long id = Thread.currentThread().getId();
        int mod = (int) (id % interceptorCacheNum);
        InterpreterWrapper[] caches = interpreterCaches.get(script);
        if (caches == null) {
            initScriptCache(script);
            caches = interpreterCaches.get(script);
        }
        InterpreterWrapper wrapper = tryToHoldInterceptor(caches[mod]);
        if (wrapper != null) {
            return wrapper;
        }
        while (true) {
            mod++;
            if (mod == interceptorCacheNum) {
                mod = 0;
            }
            wrapper = tryToHoldInterceptor(caches[mod]);
            if (wrapper != null) {
                return wrapper;
            }
        }
    }

    private InterpreterWrapper tryToHoldInterceptor(InterpreterWrapper wrapper) {
        if (wrapper.isHold()) {
            return null;
        }
        synchronized (wrapper) {
            if (wrapper.isHold()) {
                return null;
            }
            wrapper.setHold(true);
            return wrapper;
        }
    }


    private synchronized void initScriptCache(String script) {
        InterpreterWrapper[] caches = new InterpreterWrapper[interceptorCacheNum];
        for (int i = 0; i < interceptorCacheNum; i++) {
            caches[i] = new InterpreterWrapper(new Interpreter(), false);
        }
        interpreterCaches.put(script, caches);
    }

}
