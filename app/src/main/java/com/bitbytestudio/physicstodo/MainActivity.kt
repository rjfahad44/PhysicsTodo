package com.bitbytestudio.physicstodo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.bitbytestudio.physicstodo.ui.TodoScreen
import com.bitbytestudio.physicstodo.ui.theme.PhysicsTodoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhysicsTodoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ){
                    TodoScreen()
                }
            }
        }
    }
}