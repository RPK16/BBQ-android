package com.rpk16.bbqcontroller

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.appcompat.app.AppCompatActivity

class ViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CookFragment()
            1 -> HeatItUpFragment()
            2 -> MonitorFragment()
            3 -> SetUpWifiFragment()
            else -> CookFragment()
        }
    }
}