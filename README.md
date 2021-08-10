
## Android 项目根目录下文件 build.gradle 中添加

```groovy
allprojects {
	repositories {
		...
		maven { url 'https://www.jitpack.io' }
	}
}
```

## 在App的build.gradle文件中添加

### base_framework 系统api调用 [![](https://jitpack.io/v/wave-chtj/BaseFramework.svg)](https://jitpack.io/#wave-chtj/BaseFramework)

```groovy
dependencies {
         //framework API调用
		 implementation 'com.github.wave-chtj:BaseFramework:1.0.1'
}
```

### 自定义 Application

```java
//每个Module library功能描述可在页面下方查看
//别忘了在 Manifest 中通过使用这个自定义的 Application,这里有各个library的初始化
public class App extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    //base_framework 系统 api 调用
    FBaseTools.instance().create(getApplication());
  }

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    MultiDex.install(this);
  }
}

```

## ③ base_framework 系统 api 调用说明

| 编号 | 模块     | 功能                                     |
| ---- | -------- | ---------------------------------------- |
| 1    | 网络     | 以太网控制 , WIFI 控制，应用网络管理     |
| 2    | 存储空间 | sdcard 容量,TF 卡容量, ram 容量,rom 容量 |
| 3    | 升级管理 | apk 安装/卸载,固件升级                   |

| 编号 | 工具类         | 工具名称       | 实现功能                       |
| ---- | -------------- | -------------- | ------------------------------ |
| 1    | FScreentTools  | 屏幕信息工具类 | 截屏                           |
| 2    | FStorageTools  | 存储空间管理   | TF\SD\RAM\ROM 空间获取         |
| 3    | FUpgradeTools  | 升级管理       | 固件\apk 升级                  |
| 4    | FEthTools      | 以太网管理     | 开启关闭，STATIC\DHCP 模式设置 |
| 5    | FNetworkTools  | 网络工具类     | dns,流量获取                   |
| 6    | FWifiTools     | WIFI 管理      | 开启关闭                       |
| 7    | FUsbHubTools   | USB 管理       | 接入设备获取                   |
| 8    | FIPTablesTools | 应用网络管理   | 应用网络开启关闭               |