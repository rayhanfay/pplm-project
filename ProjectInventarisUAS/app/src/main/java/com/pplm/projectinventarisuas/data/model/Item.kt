package com.pplm.projectinventarisuas.data.model

import android.os.Parcelable
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
@IgnoreExtraProperties
data class Item(
    val item_id: String = "",
    val item_name: String = "",
    val item_type: String = "",
    val item_status: String = "",
    val item_description: String = ""
) : Parcelable
