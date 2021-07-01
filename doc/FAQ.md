# FQA

### 1、agent日志出现queryByAppName接口401错误

错误示例：

```aidl
 ERROR [pradar] pull plugin configs error,url:http://10.206.35.24:80/tro-web/api/application/plugins/config/queryByAppName?applicationName=stl-test&configKey=redis_expire,httpResult:{"result":"{\"msg\":\"未登陆，请重新登陆\",\"status\":401}","status":401,"success":false}
```

目前agent版本和tro-web版本没有对齐，tro-web缺少这个接口，目前不影响功能忽略即可，后续我们会优化这个问题

### 2、agent日志出"Agent提供的userAppKey在数据库中未找到记录"错误

错误示例：

```aidl
 2021-06-29 11:58:06 [pamirs] WARN  SIMULATOR: [FetchConfig] get datasource config error. url=http://10.206.35.24:80/tro-web/api/link/ds/configs/pull?appName=stl-test, status=401, result={"msg":"Agent提供的userAppKey在数据库中未找到记录","status":401}”
```

agent默认的userAppKey与控制台默认的不一致，目前需要手动替换agent的simulator.properties里的appkey配置为：``user.app.key=5b06060a-17cb-4588-bb71-edd7f65035af``，后续我们优化默认配置问题
