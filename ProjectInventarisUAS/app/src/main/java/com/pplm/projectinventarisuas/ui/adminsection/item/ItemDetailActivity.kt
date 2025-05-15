package com.pplm.projectinventarisuas.ui.adminsection.item

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pplm.projectinventarisuas.databinding.ActivityItemDetailBinding
import com.pplm.projectinventarisuas.data.model.Item

class ItemDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemDetailBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val item = intent.getParcelableExtra<Item>("item")
        item?.let {
            binding.tvItemId.text = "ID: ${it.item_id}"
            binding.tvItemName.text = "Name: ${it.item_name}"
            binding.tvItemtype.text = "Type: ${it.item_type}"
            binding.tvItemStatus.text = "Status: ${it.item_status}"
            binding.tvItemDesciption.text = "Description: ${it.item_description}"
        }
    }
}
