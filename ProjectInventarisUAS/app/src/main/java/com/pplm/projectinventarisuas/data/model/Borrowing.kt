package com.pplm.projectinventarisuas.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Borrowing(
    val borrowing_id: String = "",
    val admin_id: String = "",
    val admin_name: String = "",
    val student_id: String = "",
    val student_name: String = "",
    val item_id: String = "",
    val item_name: String = "",
    val item_type: String = "",
    val date_borrowed: String = "",
    val last_location: String = "",
    val return_time: String = "",
    val start_hour: String = "",
    val end_hour: String = "",
    val status: String = ""
) : Parcelable