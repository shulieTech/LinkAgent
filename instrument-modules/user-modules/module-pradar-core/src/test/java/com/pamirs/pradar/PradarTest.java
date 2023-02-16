package com.pamirs.pradar;


/**
 * @author Licey
 * @date 2022/8/30
 */
public class PradarTest  {

    public static void main(String[] args) {
        System.setProperty("1233", "123");
        System.out.println(Pradar.getProperty("123"));
        System.out.println(Pradar.getProperty("1233"));
        System.out.println(Pradar.getProperty("1233"));
        System.out.println(Pradar.getProperty("1233"));


    }

}