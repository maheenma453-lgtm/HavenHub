package com.example.havenhub.utils

object ValidationUtils {

    // ── Email ──────────────────────────────────────────────────────────────────
    fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return email.isNotBlank() && emailRegex.matches(email.trim())
    }

    // ── Password ───────────────────────────────────────────────────────────────
    fun isValidPassword(password: String): Boolean {
        return password.length >= Constants.MIN_PASSWORD_LENGTH
    }

    fun passwordsMatch(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }

    // ── Phone ──────────────────────────────────────────────────────────────────
    fun isValidPhone(phone: String): Boolean {
        val cleaned = phone.replace(" ", "").replace("-", "")
        val phoneRegex = Regex("^\\+?[0-9]{10,13}$")
        return phoneRegex.matches(cleaned)
    }

    // ── Name ───────────────────────────────────────────────────────────────────
    fun isValidName(name: String): Boolean {
        return name.trim().length >= 2
    }

    // ── Price ──────────────────────────────────────────────────────────────────
    fun isValidPrice(price: String): Boolean {
        return price.toDoubleOrNull()?.let { it >= Constants.MIN_PROPERTY_PRICE } ?: false
    }

    // ── Review ─────────────────────────────────────────────────────────────────
    fun isValidReview(comment: String): Boolean {
        return comment.trim().isNotBlank() && comment.length <= Constants.MAX_REVIEW_LENGTH
    }

    // ── CNIC Number ────────────────────────────────────────────────────────────
    // Valid format: XXXXX-XXXXXXX-X  (5 digits - 7 digits - 1 digit)
    // Example: 35201-1234567-8
    fun isValidCnic(cnic: String): Boolean {
        val cnicRegex = Regex("^[0-9]{5}-[0-9]{7}-[0-9]{1}$")
        return cnicRegex.matches(cnic.trim())
    }

    // Auto-formats raw digits into XXXXX-XXXXXXX-X as user types
    // Strips non-digits first, then inserts dashes at correct positions
    fun formatCnicInput(raw: String): String {
        // Keep only digits, max 13
        val digits = raw.filter { it.isDigit() }.take(13)
        return buildString {
            digits.forEachIndexed { i, c ->
                if (i == 5 || i == 12) append('-')
                append(c)
            }
        }
    }

    // Returns true only if the image MIME type is a real image (jpg/png/webp)
    // Used to block random files from being uploaded as CNIC image
    fun isValidImageMimeType(mimeType: String?): Boolean {
        return mimeType in listOf("image/jpeg", "image/png", "image/webp")
    }

    // ── Sign In Form ───────────────────────────────────────────────────────────
    fun validateSignIn(email: String, password: String): String? {
        if (!isValidEmail(email)) return "Enter a valid email address."
        if (password.isBlank()) return "Password cannot be empty."
        return null
    }

    // ── Sign Up Form ───────────────────────────────────────────────────────────
    fun validateSignUp(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        phone: String,
        cnicNumber: String,
        cnicImageProvided: Boolean
    ): String? {
        if (!isValidName(name))             return "Enter a valid full name."
        if (!isValidEmail(email))           return "Enter a valid email address."
        if (!isValidPassword(password))     return "Password must be at least ${Constants.MIN_PASSWORD_LENGTH} characters."
        if (!passwordsMatch(password, confirmPassword)) return "Passwords do not match."
        if (!isValidPhone(phone))           return "Enter a valid phone number."
        if (!isValidCnic(cnicNumber))       return "Enter a valid CNIC number (e.g. 35201-1234567-8)."
        if (!cnicImageProvided)             return "Please upload your CNIC image."
        return null
    }

    // ── Property Form ──────────────────────────────────────────────────────────
    fun validatePropertyForm(
        title: String,
        city: String,
        price: String,
        bedrooms: String,
        maxGuests: String
    ): String? {
        if (title.isBlank())                          return "Property title is required."
        if (city.isBlank())                           return "City is required."
        if (!isValidPrice(price))                     return "Enter a valid price (min Rs. ${Constants.MIN_PROPERTY_PRICE.toInt()})."
        if ((bedrooms.toIntOrNull() ?: 0) < 1)        return "Enter valid number of bedrooms."
        if ((maxGuests.toIntOrNull() ?: 0) < 1)       return "Enter valid max guests."
        return null
    }
}
