package com.example.mycompose.imagerecognizer.data

data class Animal(
    val id: String,
    val number: String,
    val name: String,
    val type: String,
    val photos: List<String>,
    val features: List<FloatArray>
)

val Animal.title: String
    get() = if (name.isBlank()) "$type رقم $number" else "$name ($type $number)"

val ANIMAL_TYPES = listOf("نعجة", "معزة", "كبش", "تيس", "حمل", "جدي")

class Candidate(val animal: Animal, val score: Float, val byNumber: Boolean)
