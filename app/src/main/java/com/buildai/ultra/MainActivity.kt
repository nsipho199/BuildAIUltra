package com.buildai.ultra

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.buildai.ultra.data.AppDatabase
import com.buildai.ultra.data.SettingsManager
import com.buildai.ultra.databinding.ActivityMainBinding
import com.buildai.ultra.viewmodel.BuildViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        val viewModel = ViewModelProvider(this)[BuildViewModel::class.java]
        viewModel.init(SettingsManager(this), AppDatabase.getInstance(this))
    }
}
