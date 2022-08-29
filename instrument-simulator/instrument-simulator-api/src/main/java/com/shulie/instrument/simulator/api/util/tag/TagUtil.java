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
package com.shulie.instrument.simulator.api.util.tag;

/**
 * @author Licey
 * @date 2021/9/26
 */
public class TagUtil {
    private static final int[] tags = new int[]{1, 2, 4, 8, 16, 32, 64, 128};

    public static boolean checkTag(int index, int data) {
        return (data & tags[index - 1]) == tags[index - 1];
    }

    public static int toTag(boolean... args) {
        if (args.length > tags.length) {
            throw new RuntimeException("format tag fail, tags length < args length");
        }
        int res = 0;
        for (int i = 0; i < args.length; i++) {
            if (args[i]) {
                res = res | tags[i];
            }
        }
        return res;
    }

    public static void main(String[] args) {
        System.out.println(toTag(false, false));
        System.out.println(toTag(true, false));
        System.out.println(toTag(true, true, true));
        System.out.println(toTag(false, false, true));
        System.out.println(toTag(false, false, false, true));

        System.out.println(checkTag(2, 3));
        System.out.println(checkTag(2, 1));
        System.out.println(checkTag(1, 5));

        System.out.println(checkTag(4, 7));
        System.out.println(checkTag(4, 9));
    }
}
