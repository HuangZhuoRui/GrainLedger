package com.vincent.grainledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.vincent.grainledger.ui.navigation.AppNavHost
import com.vincent.grainledger.ui.viewmodel.MainViewModel

/**
 * 余粮主入口 Activity。
 *
 * 启用边到边沉浸式全屏布局，初始化全局 ViewModel 并渲染全局导航图。
 */
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppNavHost(
                viewModel = mainViewModel
            )
        }
    }
}