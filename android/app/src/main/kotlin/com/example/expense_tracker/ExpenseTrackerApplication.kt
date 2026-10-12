package com.example.expense_tracker

import android.app.Application
import androidx.room.Room
import com.example.expense_tracker.data.AppDatabase
import com.example.expense_tracker.data.ExpenseRepository

class ExpenseTrackerApplication : Application() {
    val repository: ExpenseRepository by lazy {
        ExpenseRepository(
            Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "smart-expenses.db",
            ).build(),
        )
    }
}
