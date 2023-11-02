package com.pamirs.pradar.script.bsh;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.NameSpace;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GlobalCacheBshScriptEvaluator implements ScriptEvaluator {

    private final static Logger logger = LoggerFactory.getLogger("ThreadLocalScriptEvaluator-LOGGER");

    private ClassLoader classLoader;

    private static final Interpreter interpreter = new Interpreter();
    /**
     * Construct a new BshScriptEvaluator.
     */
    public GlobalCacheBshScriptEvaluator() {
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
        try {
            NameSpace ns = new NameSpace(interpreter.getNameSpace(), String.valueOf(Thread.currentThread().getId()));
            interpreter.setClassLoader(this.classLoader);
            if (arguments != null) {
                for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                    interpreter.set(entry.getKey(), entry.getValue());
                }
            }
            return interpreter.eval(script,ns);
        } catch (EvalError ex) {
            throw new RuntimeException(script, ex);
        }
    }

    @Override
    public Object evaluate(ClassLoader classLoader, String script, Map<String, Object> arguments) {
        try {
            NameSpace ns = new NameSpace(interpreter.getNameSpace(), String.valueOf(Thread.currentThread().getId()));
            // 非必要不刷新ClassLoader
            ClassLoader externalClassLoader = ReflectionUtils.get(interpreter.getClassManager(), "externalClassLoader");
            if (externalClassLoader != classLoader) {
                interpreter.setClassLoader(classLoader);
            }
            if (arguments != null) {
                for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                    ns.setVariable(entry.getKey(), entry.getValue(),false);
                }
            }
            return interpreter.eval(script, ns);
        } catch (Exception ex) {
            throw new RuntimeException(script, ex);
        }
    }
}
