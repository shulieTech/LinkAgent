注意！！！！！
每次更新插件请都更新内容到以下内容，
simulator 模块，
新增模块版本信息，初始版本为1.0.0，README.md为模块更新内容描述文件，


5.3.0.1 matchConfig的调整升级

5.1.0.0版本：
支持影子库账密前缀处理。
仿真系统新增配置项：
shadow.datasource.account.prefix（前缀参数，默认是PT_）
shadow.datasource.account.suffix（后缀参数，默认是空字符串）
说明：假设原业务库账号为 admin，密码为 password。现影子库页面上无需配置账号密码，会自动以 PT_admin 作为影子库账号, PT_password 作为影子库密码去连接影子库。
