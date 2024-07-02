package com.akbas.orm.config

data class DBConnectionDetails(
    val url: String,
    val driverClassName: String,
    val userName: String,
    val password: String
)
