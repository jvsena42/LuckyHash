package com.github.luckyhash

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.github.luckyhash.domain.MiningService
import com.github.luckyhash.ui.navigation.AppNavHost
import com.github.luckyhash.ui.theme.LuckyHashTheme
import org.koin.compose.KoinContext

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // We'll continue even if permission is denied, but notifications won't show
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        enableEdgeToEdge()
        setContent {
            KoinContext {
                LuckyHashTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        AppNavHost(
                            navController = navController,
                            startService = {
                                startService(
                                    Intent(
                                        this@MainActivity,
                                        MiningService::class.java
                                    )
                                )
                            },
                            stopService = {
                                //TODO IMPLEMENT
                            }
                        )
                    }
                }
            }
        }
    }
}
