package com.example.pareshgeneral.data

import java.util.UUID

data class Rental(
    val id: String = UUID.randomUUID().toString(),
    val date: String = "",
    val name: String = "",
    val contact: String = "",
    val jewelryNo: String = "",
    val jewelryDetails: String = "",
    val deliveryDate: String = "",
    val returnDate: String = "",
    val rent: Double = 0.0,
    val advance: Double = 0.0,
    val balance: Double = 0.0,
    val refundAmount: Double = 0.0,
    val images: List<String> = emptyList(),
    val isReceived: Boolean = false
)
