package com.pamirs.attach.plugin.activemqv2.util;

import com.pamirs.pradar.Pradar;
import org.apache.activemq.command.ActiveMQDestination;

/**
 * @author guann1n9
 * @date 2023/12/22 6:36 PM
 */
public class ActiveMQDestinationUtil {



    public static ActiveMQDestination mappingShadowDestination(ActiveMQDestination origin){
        String qualifiedName = origin.getQualifiedName();
        if(qualifiedName.contains(Pradar.getClusterTestPrefix())){
            //已是影子queue
            return origin;
        }
        String[] split = qualifiedName.split("//");
        if(split.length != 2){
            return null;
        }
        String bizQueue = split[1];
        String shadowQueue = Pradar.getClusterTestPrefix() + bizQueue;
        String shadowName = split[0]+"//"+shadowQueue;
        byte b = -1;
        return ActiveMQDestination.createDestination(shadowName, b);

    }
}
