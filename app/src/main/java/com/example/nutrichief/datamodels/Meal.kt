package com.example.nutrichief.datamodels

data class Meal(
    val id: String,
    val title: String,
    val desc: String,
    val calories: Float,
    val protein: Float,
    val fat: Float,
    val ingredients: List<String>,
    val categories: List<String>,
    val rating: Float,
    val directions: List<String>
)
