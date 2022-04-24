注意！！！！！
每次更新插件请都更新内容到以下内容，
catalina中间件支持模块，
新增模块版本信息，初始版本为1.0.0，README.md为模块更新内容描述文件，

2.0.0.1 版本修复以下bug
tomcat修复了以下写法会一直死循环的情况，无法响应请求，主要是while循环里这段代码
public String index(HttpServletRequest req){
userService.print("angju");
final byte[] buffer = new byte[16384];
boolean hasoutbody = true;
try {

            if (hasoutbody) {
                while (true) {
                    final int read = req.getInputStream().read(buffer);
                    if (read <= 0) {
                        break;
                    }
//                    conn.getOutputStream().write(buffer, 0, read);
}
}
}catch (IOException e){

        }

        return "{\"code\":200,\"msg\":\"ok\",\"data\":[\"JSON.IM\",\"json格式化\"]}";
    }