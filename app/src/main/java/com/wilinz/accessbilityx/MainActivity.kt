package com.wilinz.accessbilityx

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.wilinz.accessbilityx.app.launchAppByName
import com.wilinz.accessbilityx.app.launchAppPackage
import com.wilinz.accessbilityx.ui.theme.AccessbilityXTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val TAG = "MainActivity.kt"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AccessbilityXTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(Modifier.fillMaxSize()) {
                        Button(onClick = {
                            goAccessibilityServiceSettings()
                        }) {
                            Text(text = "打开无障碍")
                        }
                        Button(onClick = {
                            Toast.makeText(this@MainActivity, "测试按钮1被点击", Toast.LENGTH_SHORT).show()
                        }) {
                            Text(text = "测试按钮")
                        }
                        Button(onClick = {
                            Toast.makeText(this@MainActivity, "测试按钮2被点击", Toast.LENGTH_SHORT).show()
                        }) {
                            Text(text = "测试按钮")
                        }
                        Button(onClick = {
                            lifecycleScope.launch {
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
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AccessbilityXTheme {
        Greeting("Android")
    }
}