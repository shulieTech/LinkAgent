package io.shulie.instrument.module;

import com.shulie.instrument.simulator.api.util.Castor;

/**
 * @author Licey
 * @date ${DATE}
 */
public class Main {
    public static void main(String[] args) throws NoSuchMethodException {
        System.out.println("Hello world!");
        Castor.canCast(Main.class.getDeclaredMethod("ttt").getReturnType(),null);
        System.out.println(Main.class.getDeclaredMethod("ttt").getReturnType());
    }

    public static void ttt(){

    }
}