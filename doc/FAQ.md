# FQA

### 1、agent日志出"Agent提供的userAppKey在数据库中未找到记录"错误

错误示例：

```aidl
 2021-06-29 11:58:06 [pamirs] WARN  SIMULATOR: [FetchConfig] get datasource config error. url=http://10.206.35.24:80/tro-web/api/link/ds/configs/pull?appName=stl-test, status=401, result={"msg":"Agent提供的userAppKey在数据库中未找到记录","status":401}”
```

agent默认的userAppKey与控制台默认的不一致，目前需要手动替换agent的simulator.properties里的appkey配置为：``user.app.key=5b06060a-17cb-4588-bb71-edd7f65035af``
