package com.example.scrolltracker.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scrolltracker.data.PreferenceManager
import com.example.scrolltracker.databinding.ActivityGoalsBinding

class GoalsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoalsBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var adapter: GoalsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoalsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)

        setupRecyclerView()

        binding.btnAddGoal.setOnClickListener {
            val targetGoal = binding.etNewGoal.text.toString().trim()
            if (targetGoal.isNotEmpty()) {
                preferenceManager.addGoal(targetGoal)
                binding.etNewGoal.text.clear()
                refreshGoals()
            } else {
                Toast.makeText(this, "Please enter a valid goal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = GoalsAdapter(preferenceManager.getGoals()) { goalToDelete ->
            preferenceManager.removeGoal(goalToDelete)
            refreshGoals()
        }
        binding.rvGoals.layoutManager = LinearLayoutManager(this)
        binding.rvGoals.adapter = adapter
    }

    private fun refreshGoals() {
        val goals = preferenceManager.getGoals()
        adapter.updateGoals(goals)
    }
}
