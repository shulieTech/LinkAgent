package io.shulie.instrument.module;

import com.shulie.instrument.simulator.api.util.Castor;

import java.lang.reflect.Method;

/**
 * @author Licey
 * @date ${DATE}
 */
public class Main {
    public static void main(String[] args) throws NoSuchMethodException {
        System.out.println("Hello world!");
        Castor.canCast(Main.class.getDeclaredMethod("ttt").getReturnType(),null);
        System.out.println(Main.class.getDeclaredMethod("ttt").getReturnType());

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("aaa,");
        stringBuilder.deleteCharAt(stringBuilder.length()-1);
        System.out.println(stringBuilder);

        for (Method declaredMethod : Bbb.class.getDeclaredMethods()) {
            System.out.println(declaredMethod.getName());

        }
    }

    public static void ttt(){

    }

    static class Aaa{
        public void Apublic(){

        }

        private void Aprivate(){

        }
    }

    static class Bbb extends Aaa{
        public void Bpublic(){

        }

        private void Bprivate(){

        }
    }
}