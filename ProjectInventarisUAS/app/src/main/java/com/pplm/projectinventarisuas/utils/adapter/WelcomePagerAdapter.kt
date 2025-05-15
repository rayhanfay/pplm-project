package com.pplm.projectinventarisuas.utils.adapter

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class WelcomePagerAdapter(
    activity: AppCompatActivity,
    private val fragments: List<Fragment>
) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = fragments.size
    override fun createFragment(position: Int): Fragment = fragments[position]
}
