注意！！！！！
每次更新插件请都更新内容到以下内容，
shadow-job中间件支持模块，
新增模块版本信息，初始版本为1.0.0，README.md为模块更新内容描述文件，

2.0.0.1版本

将注释掉的org.springframework.context.support.ApplicationContextAwareProcessor#invokeAwareInterfaces放开

2.0.0.2版本

1、重写xxlJob实现逻辑解决不同classLoad加载导致抛 ClassNotFoundError。
2、解决 IJobHandler 未被Spring管理导致获取不到对象问题