package com.example.excusemyfrenchcompose.data.model

import kotlinx.serialization.Serializable

@Serializable
data class InsultResponse(
    val insult: Insult = Insult(),
    val image: Image = Image()
)

@Serializable
data class Insult(
    val text: String = "No insult available",
    val index: Int = -1
)

@Serializable
data class Image(
    val data: String = "",
    val mimetype: String = "",
    val indexImg: Int = -1
)
