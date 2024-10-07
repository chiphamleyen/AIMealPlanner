package com.example.nutrichief.datamodels

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.Date

@JsonIgnoreProperties(ignoreUnknown = true)
data class User(
    var name: String?,
    var email: String?,
    var password: String?,
    var gender: String?,
    var height: Float?,
    var weight: Float?,
    var date_of_birth: Date?,
    var allergies: List<String>?,
    var dietary_preferences: List<String>?,
    var profile_picture: String?
)