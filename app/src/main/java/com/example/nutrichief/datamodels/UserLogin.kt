package com.example.nutrichief.datamodels

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.Date

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserLogin(
    var email: String?,
    var password: String?,
)