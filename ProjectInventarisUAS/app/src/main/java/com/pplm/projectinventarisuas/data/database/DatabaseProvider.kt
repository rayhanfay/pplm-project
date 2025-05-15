package com.pplm.projectinventarisuas.data.database

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object DatabaseProvider {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    fun getDatabaseReference(): DatabaseReference {
        return database
    }
}
