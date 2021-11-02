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
package com.shulie.instrument.simulator.core.enhance;

import com.shulie.instrument.simulator.api.listener.ext.BuildingForListeners;
import com.shulie.instrument.simulator.core.enhance.weaver.asm.AsmCodeEnhancer;
import com.shulie.instrument.simulator.core.util.AsmUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.Opcodes.ASM7;

public class EventEnhancer implements Enhancer {
    private final static int CLASS_VERSION_15 = 49;

    private int getClassMajorVersion(byte[] data) {
        return (short) (((data[6] & 0xFF) << 8) | (data[6 + 1] & 0xFF));
    }

    private ClassWriter createClassWriter(final ClassLoader targetClassLoader,
                                          final ClassReader cr,
                                          final byte[] byteCodeArray) {
        int majorVersion = getClassMajorVersion(byteCodeArray);
        int flags = ClassWriter.COMPUTE_FRAMES;
        if (majorVersion <= CLASS_VERSION_15) {
            // java 1.5 and less.
            flags = ClassWriter.COMPUTE_MAXS;
        }
        return new ClassWriter(cr, flags) {

            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                return AsmUtils.getCommonSuperClass(type1, type2, targetClassLoader);
            }

        };
    }

    @Override
    public byte[] toByteCodeArray(final ClassLoader targetClassLoader,
                                  final byte[] byteCodeArray,
                                  final Map<String, Set<BuildingForListeners>> signCodes) {
        final ClassReader cr = new ClassReader(byteCodeArray);
        final ClassWriter cw = createClassWriter(targetClassLoader, cr, byteCodeArray);
        cr.accept(
                new AsmCodeEnhancer(
                        ASM7, cw,
                        cr.getClassName(),
                        signCodes
                ),
                EXPAND_FRAMES
        );

        return cw.toByteArray();
    }
}
