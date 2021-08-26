/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/03/29 7:11 下午
 */
Thread.currentThread().contextClassLoader.getResource("configName").eachLine {
    it << 1
    def tmp = it.charAt(0).toUpperCase().toString() + it.substring(1)
    int idx = tmp.indexOf("-")
    while (idx > 0) {
        tmp = tmp.substring(0, idx) + tmp.charAt(idx + 1).toUpperCase().toString() + tmp.substring(idx + 2)
        idx = tmp.indexOf("-")
    }
    def properties = tmp.charAt(0).toLowerCase().toString() + tmp.substring(1)
    println """
String $properties = ptDataSourceConf.getProperty("$it");
    if ($properties != null) {
        target.set$tmp($properties);
    } else {
        target.set$tmp(sourceDatasource.get$tmp());
    }
"""


}