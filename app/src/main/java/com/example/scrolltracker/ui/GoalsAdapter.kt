package com.example.scrolltracker.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.scrolltracker.databinding.ItemGoalBinding

class GoalsAdapter(
    private var goals: List<String>,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<GoalsAdapter.GoalViewHolder>() {

    inner class GoalViewHolder(private val binding: ItemGoalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(goal: String) {
            binding.tvGoalText.text = goal
            binding.ivDelete.setOnClickListener {
                onDeleteClick(goal)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val binding = ItemGoalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        holder.bind(goals[position])
    }

    override fun getItemCount(): Int = goals.size

    fun updateGoals(newGoals: List<String>) {
        goals = newGoals
        notifyDataSetChanged()
    }
}
