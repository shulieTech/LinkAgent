package io.shulie.instrument.module.messaging.consumer.http;

public class JdkHttpTester {


    public static void main(String[] args) {
        String s = HttpUtil.normalGet("http://localhost:8081/druid/mysql/demo?name=bob");
        System.out.println(s);

    }

}
