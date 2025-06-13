# 短链生成服务 - ShortUrl

## 服务在线使用及开发环境

> https://ivii.cn
>
> JDK 8 + MySQL 5.7 + IDEA 2021

## 使用

* 确保java8及以上环境，在[releases](https://github.com/rawchen/short-url/releases)页面下载jar运行程序。
* 通过`java -jar ShortUrl.jar`运行该程序，默认访问地址：[http://localhost:8989](http://localhost:8989)。
* 需部署MySQL数据库，帐号root密码root，端口3306。新建名为`short_url`的库，导入`short_url.sql`
* Linux部署使用如下命令：
```bash
nohup java -jar -Xmx200m short-url.jar >> ShortUrl.log  &
```

## 开发

* 确保java8及Maven环境，下载项目后通过IDEA打开项目也可直接修改相关文本。
* 可配置application.yml中的端口号，数据库连接信息等，通过IDEA运行这个Spring Boot项目。
* 不想使用IDEA也可文本编辑后Maven打包成jar运行，打包命令`mvn clean package`。


## 功能

- [x] 有效期
- [x] 访问密码
- [x] 随机短链
- [x] 生成二维码
- [x] 接口限流

## 案例截图

> https://ivii.cn