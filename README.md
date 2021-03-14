# kl_crawler
## 基于Appium的App UI遍历 & Monkey 工具

* 工程名：http://git.sankuai.com/projects/KLQA/repos/kl_crawler/browse 
* 支持 Appium1.16.0-beta.3及1.17.0-beta.1(本地此appium版本能稳定在真机上执行)

## 定时类
* util目录下的TimeTest()，可以run这个文件，达到定时执行的目的

## 也可以运行shell脚本
* 先启动appium ，如 appium  --session-override -p 4723
```
 --session-override  覆盖之前的session;
 -p 设定appium server的端口;不加参数默认为4723;

```
   
## 执行run.sh文件
```
java -jar target/kl_crawler-2.0.jar -f config_kl.yml -t 4723  -u Z9K7ONN799999999
-jar 指定jar包
-f 指定yml配置文件 若无此参数 默认为config.yml 
-t 指定appium server的端口（此项为可选项，默认值是4723)
-u 指定设备udid
```
```
快驴商城app执行run_kl.sh
驼铃app执行run_camelbd.sh
司机app执行run_delivery.sh
```

## kl_crawler-2.0.jar
* 在工程目录下执行mvn package，可以得到kl_crawler-2.0.jar
* 使用工程中已上传的jar包


## config_kl.yml  （快驴遍历的配置）
* 执行app自动遍历的配置文件,其中比较关键的字段
```
ANDROID_PACKAGE:待执行的App包名，同Android App自动化测试配置一样
ANDROID_MAIN_ACTIVITY：启动的Activity，同Android App自动化测试配置一样
ANDROID_BOTTOM_TAB_BAR_ID：安卓遍历的起点
IOS_BUNDLE_ID：待执行的App包名，同Ios App自动化测试配置一样
IOS_BUNDLE_NAME：手机桌面上显示的APP的名字（这个很重要，判断是否有Crash）
IOS_BOTTOM_TAB_BAR_TYPE：Ios遍历的起点
PRESS_BACK_TEXT_LIST：UI元素中出现下列文字时 触发back键，应用场景：在不想对这些文案进行操作，如拨打电话等跳出待测app
PRESS_BACK_PACKAGE_LIST:当App跳转到以下app时 触发back键，应用场景：如点击分享的app
PRESS_BACK_ACTIVITY_LIST：当前遇到以下Activity时 触发back键 (Android only)，应用场景：如调用系统相机拍照
ANDROID_VALID_PACKAGE_LIST：除了APP本身的包名外 根据以下包名判断是否跳出了待测APP,当app跳转到以下app时被认为是合法，会继续遍历操作，否则，就当做是Crash
IOS_VALID_BUNDLE_LIST：除了APP本身的包名外 根据以下包名判断是否跳出了待测APP,当app跳转到以下app时被认为是合法，会继续遍历操作，否则，就当做是Crash
ITEM_BLACKLIST：不点击包含以下文本的控件，应用场景：测试包的Debug 面板
IGNORE_CRASH:是否忽略Crash,设为true时,crash后会重启app然后继续遍历
CRAWLER_RUNNING_TIME:遍历运行时间限制(分钟)
USER_LOGIN_INTERVVAL:每个UI变化X次时，检测是否需要自动登录
ENABLE_AUTO_LOGIN：#开启自动登录功能
 - ANDROID_AccountPasswordLogin:
        XPATH: '//*[@text="账号密码登录"]'
        ACTION: click
        VALUE: '1'
 - ANDROID_USERNAME:
        XPATH: '//*[@text="请输入账号"]'
        ACTION: input
        VALUE: 'kuailv13031190943'
```

## 多设备执行
```
同时开启多个终端的appium server，然后在用命令行执行时指定对应的端口和设备，就可以多设备同时执行
java -jar target/kl_crawler-2.0.jar -f config.yml -t 4725  -u Z9K7ONN799999999
java -jar target/kl_crawler-2.0.jar -f config.yml -t 4723  -u 127.0.0.1:62001
```


## 一些技术文档
* Appium Java-Client API http://appium.github.io/java-client/
* iOS多机远程控制技术 https://mp.weixin.qq.com/s/rN2xcO9gNIAoeY71NX_HZw
* http://appium.io/docs/en/commands/mobile-command/
* https://appiumpro.com/editions/12
* SpringAOP https://blog.csdn.net/zknxx/article/details/53240959
* Ui_crawler环境搭建及基本使用说明： https://testerhome.com/topics/14490 



