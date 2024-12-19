# AccessibilityX 无障碍扩展库
[![](https://jitpack.io/v/wilinz/AccessibilityX.svg)](https://jitpack.io/#wilinz/AccessibilityX)

## 使用方法：
无障碍服务继承 此依赖库的 AccessibilityxService
( Accessibility后面带有一个x )

具体用法查看app模块示例

## Gradle引用:

Add it in your root build.gradle at the end of repositories:

```kotlin
allprojects {
    repositories {
        // 其他仓库配置
        maven("https://jitpack.io")
    }
}
```

Step 2. Add the dependency

```kotlin
dependencies {
    implementation("com.github.wilinz:AccessibilityX:0.0.3")
}
```
代码示例
```kotlin
class AutoAccessibilityService : AccessibilityxService() {

    companion object {
        var instance: AutoAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Toast.makeText(this, "无障碍已打开", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        super.onAccessibilityEvent(event)
    }


    override fun onInterrupt() {
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
```
```kotlin
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }

    private fun goAccessibilityServiceSettings() {
        // 在这里放置您的方法，打开无障碍服务的设置页面
        Toast.makeText(this, "打开无障碍服务设置", Toast.LENGTH_SHORT).show()
        // 实际代码可能会跳转到无障碍设置页面，例如:
        // startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}

@Composable
fun MyApp() {
    Column {
        Button(onClick = {
            // 调用打开无障碍服务设置的方法
            (LocalContext.current as MainActivity).goAccessibilityServiceSettings()
        }) {
            Text(text = "打开无障碍")
        }

        Button(onClick = {
            // 使用生命周期作用域执行任务
            (LocalContext.current as MainActivity).lifecycleScope.launch {
                val auto = AutoAccessibilityService.instance
                launchAppByName("Autox.js v6")

                delay(500)

                auto?.untilFindOne {
                    it.text1 == "管理"
                }?.ensureClick()

                delay(500)

                auto?.untilFindOne {
                    it.text1 == "文档"
                }?.ensureClick()

                delay(500)

                auto?.untilFindOne {
                    it.text1 == "主页"
                }?.ensureClick()
            }
        }) {
            Text(text = "测试无障碍点击Autoxjs的底部按钮")
        }
    }
}
```
AndroidManifest.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
        tools:ignore="QueryAllPackagesPermission" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.AccessbilityX"
        tools:targetApi="31">

        <service android:name=".AutoAccessibilityService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:label="AccessibilityX"
            android:exported="true">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.AccessbilityX">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```
