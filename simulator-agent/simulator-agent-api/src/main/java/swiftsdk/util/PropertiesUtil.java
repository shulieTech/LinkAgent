//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package swiftsdk.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class PropertiesUtil {
    private static Map<String, String> propertyMap = new HashMap();

    public PropertiesUtil() {
    }

    public static String getProperty(String propName, String defaultValue) {
        String propValue = getStringProperty(propName);
        return propValue != null ? propValue : defaultValue;
    }

    public static String getStringProperty(String propName) {
        if (StringUtils.isEmpty(propName)) {
            throw new IllegalArgumentException("invalid parameter");
        } else {
            return (String)propertyMap.get(propName);
        }
    }

    public static int getIntProperty(String propName, int defaultValue) {
        try {
            String value = getStringProperty(propName);
            int IntValue = value != null ? Integer.parseInt(value) : defaultValue;
            return IntValue;
        } catch (NumberFormatException var4) {
            return defaultValue;
        } catch (IllegalArgumentException var5) {
            return defaultValue;
        }
    }

    static {
        Properties properties = new Properties();

        try {
            properties.load(PropertiesUtil.class.getClassLoader().getResourceAsStream("ossconfig.properties"));
            Iterator var1 = properties.keySet().iterator();

            while(var1.hasNext()) {
                Object key = var1.next();
                propertyMap.put((String)key, properties.getProperty((String)key));
            }
        } catch (Exception var3) {
        }

    }
}
