package com.example.growagarden

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigationrail.NavigationRailView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.example.growagarden.fragments.*
import com.example.growagarden.viewmodel.GardenViewModel
import com.example.growagarden.utils.NotificationPermissionHelper

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private var tabLayout: TabLayout? = null
    private var navigationRail: NavigationRailView? = null
    private lateinit var fabRefresh: FloatingActionButton
    private lateinit var viewModel: GardenViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[GardenViewModel::class.java]

        setupViews()
        setupViewPager()
        setupRefreshButton()
        requestNotificationPermission()
    }

    private fun setupViews() {
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        navigationRail = findViewById(R.id.navigationRail)
        fabRefresh = findViewById(R.id.fabRefresh)
    }

    private fun setupViewPager() {
        val adapter = GardenPagerAdapter(this)
        viewPager.adapter = adapter

        tabLayout?.let { tabs ->
            TabLayoutMediator(tabs, viewPager) { tab, position ->
                setupTabContent(tab, position)
            }.attach()
        }

        navigationRail?.let { rail ->
            rail.setOnItemSelectedListener { menuItem ->
                val position = getPositionFromMenuItem(menuItem)
                if (position != -1) {
                    viewPager.currentItem = position
                    true
                } else {
                    false
                }
            }

            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    val menuItemId = getMenuItemIdFromPosition(position)
                    if (menuItemId != -1) {
                        rail.selectedItemId = menuItemId
                    }
                }
            })
        }
    }

    private fun setupRefreshButton() {
        fabRefresh.setOnClickListener {
            viewModel.refreshData()
        }
    }

    private fun requestNotificationPermission() {
        if (!NotificationPermissionHelper.hasNotificationPermission(this)) {
            NotificationPermissionHelper.requestNotificationPermission(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            NotificationPermissionHelper.NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    NotificationPermissionHelper.showPermissionDeniedDialog(this)
                }
            }
        }
    }

    private fun setupTabContent(tab: TabLayout.Tab, position: Int) {
        when (position) {
            0 -> {
                tab.text = "All Stocks"
                tab.setIcon(R.drawable.ic_all_stocks)
            }
            1 -> {
                tab.text = "Gear"
                tab.setIcon(R.drawable.ic_gear)
            }
            2 -> {
                tab.text = "Seeds"
                tab.setIcon(R.drawable.ic_seeds)
            }
            3 -> {
                tab.text = "Eggs"
                tab.setIcon(R.drawable.ic_eggs)
            }
            4 -> {
                tab.text = "Cosmetics"
                tab.setIcon(R.drawable.ic_cosmetics)
            }
            5 -> {
                tab.text = "Honey"
                tab.setIcon(R.drawable.ic_honey)
            }
            6 -> {
                tab.text = "Night"
                tab.setIcon(R.drawable.ic_night)
            }
            7 -> {
                tab.text = "Weather"
                tab.setIcon(R.drawable.ic_weather)
            }
        }
    }

    private fun getPositionFromMenuItem(menuItem: MenuItem): Int {
        return when (menuItem.itemId) {
            R.id.nav_all_stocks -> 0
            R.id.nav_gear -> 1
            R.id.nav_seeds -> 2
            R.id.nav_eggs -> 3
            R.id.nav_cosmetics -> 4
            R.id.nav_honey -> 5
            R.id.nav_night -> 6
            R.id.nav_weather -> 7
            else -> -1
        }
    }

    private fun getMenuItemIdFromPosition(position: Int): Int {
        return when (position) {
            0 -> R.id.nav_all_stocks
            1 -> R.id.nav_gear
            2 -> R.id.nav_seeds
            3 -> R.id.nav_eggs
            4 -> R.id.nav_cosmetics
            5 -> R.id.nav_honey
            6 -> R.id.nav_night
            7 -> R.id.nav_weather
            else -> -1
        }
    }

    private inner class GardenPagerAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {

        override fun getItemCount(): Int = 8

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> AllStocksFragment()
                1 -> StockFragment.newInstance("gear")
                2 -> StockFragment.newInstance("seeds")
                3 -> StockFragment.newInstance("eggs")
                4 -> StockFragment.newInstance("cosmetics")
                5 -> StockFragment.newInstance("honey")
                6 -> StockFragment.newInstance("night")
                7 -> WeatherFragment()
                else -> AllStocksFragment()
            }
        }
    }
}