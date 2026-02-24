package com.example.havenhub.utils
object ValidationUtils {

    // ── Email ─────────────────────────────────────────────────────────
    fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return email.isNotBlank() && emailRegex.matches(email.trim())
    }

    // ── Password ──────────────────────────────────────────────────────
    fun isValidPassword(password: String): Boolean {
        return password.length >= Constants.MIN_PASSWORD_LENGTH
    }

    fun passwordsMatch(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }

    // ── Phone ─────────────────────────────────────────────────────────
    fun isValidPhone(phone: String): Boolean {
        val cleaned = phone.replace(" ", "").replace("-", "")
        val phoneRegex = Regex("^\\+?[0-9]{10,13}$")
        return phoneRegex.matches(cleaned)
    }

    // ── Name ──────────────────────────────────────────────────────────
    fun isValidName(name: String): Boolean {
        return name.trim().length >= 2
    }

    // ── Price ─────────────────────────────────────────────────────────
    fun isValidPrice(price: String): Boolean {
        return price.toDoubleOrNull()?.let { it >= Constants.MIN_PROPERTY_PRICE } ?: false
    }

    // ── Review ────────────────────────────────────────────────────────
    fun isValidReview(comment: String): Boolean {
        return comment.trim().isNotBlank() && comment.length <= Constants.MAX_REVIEW_LENGTH
    }

    // ── Sign In Form ──────────────────────────────────────────────────
    fun validateSignIn(email: String, password: String): String? {
        if (!isValidEmail(email)) return "Enter a valid email address."
        if (password.isBlank()) return "Password cannot be empty."
        return null
    }

    // ── Sign Up Form ──────────────────────────────────────────────────
    fun validateSignUp(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        phone: String
    ): String? {
        if (!isValidName(name)) return "Enter a valid full name."
        if (!isValidEmail(email)) return "Enter a valid email address."
        if (!isValidPassword(password)) return "Password must be at least ${Constants.MIN_PASSWORD_LENGTH} characters."
        if (!passwordsMatch(password, confirmPassword)) return "Passwords do not match."
        if (!isValidPhone(phone)) return "Enter a valid phone number."
        return null
    }

    // ── Property Form ─────────────────────────────────────────────────
    fun validatePropertyForm(
        title: String,
        city: String,
        price: String,
        bedrooms: String,
        maxGuests: String
    ): String? {
        if (title.isBlank()) return "Property title is required."
        if (city.isBlank()) return "City is required."
        if (!isValidPrice(price)) return "Enter a valid price (min Rs. ${Constants.MIN_PROPERTY_PRICE.toInt()})."
        if ((bedrooms.toIntOrNull() ?: 0) < 1) return "Enter valid number of bedrooms."
        if ((maxGuests.toIntOrNull() ?: 0) < 1) return "Enter valid max guests."
        return null
    }
}