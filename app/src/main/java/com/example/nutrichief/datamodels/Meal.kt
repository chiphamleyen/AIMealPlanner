package com.example.nutrichief.datamodels

data class Meal(
    val title: String,
    val ingredients: List<String>,
    val directions: List<String>,
    val calories: Double?,
    val fat: Double?,
    val protein: Double?,
    val sodium: Double?,
    val rating: Double?,
    val categories: List<String>
)
