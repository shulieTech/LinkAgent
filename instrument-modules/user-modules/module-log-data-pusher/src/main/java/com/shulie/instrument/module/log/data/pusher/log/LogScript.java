package com.shulie.instrument.module.log.data.pusher.log;

import org.apache.commons.lang.StringUtils;
import java.util.List;

/**
 * @author guann1n9
 * @date 2022/4/13 2:33 PM
 * 从后往前 指定关键字 读取n行 shell
 */
public class LogScript {


    /**
     * 第一行 为目标文件的全路径
     * 第二行 为目标文件名
     *
     * 后续为日志过滤读取结果
     */
    private static final String SCRIPT = "#!/bin/sh\n" +
            "\n" +
            "path=%s\n" +
            "\n" +
            "if [ ! $path ]; then\n" +
            "    exit 1\n" +
            "fi\n" +
            "\n" +
            "log=$(echo $path  | awk -F  '/' '{print $NF}')\n" +
            "\n" +
            "result=%s\n" +
            "\n" +
            "target=%s\n" +
            "\n" +
            "echo $path > $target\n" +
            "\n" +
            "echo $log >>  $target\n" +
            "\n" +
            "%s $path  %s  %s  > $result\n" +
            "\n" +
            "exit 1\n";

    /**
     *
     * @param logFile  获取指定读取文件的脚本
     * @param resultLog  结果输出目标文件
     * @param grepParam grep参数
     * @param rowNum  读取行数
     * @return
     */
    public static String script(String logFile, String resultLog, String targetFile,List<String> grepParam, int rowNum){

        StringBuilder builder = new StringBuilder();
        for (String param : grepParam) {
            builder.append(" | grep ").append("'").append(param).append("'");
        }
        String grep = builder.toString();
        String script;
        if (StringUtils.isEmpty(grep)) {
            script = String.format(SCRIPT, logFile, resultLog, targetFile, "tail -n "+rowNum, grep, "");
        }else {
            script = String.format(SCRIPT, logFile, resultLog, targetFile, "tac ", grep, " -m"+rowNum);
        }
        return script;
    }


}
