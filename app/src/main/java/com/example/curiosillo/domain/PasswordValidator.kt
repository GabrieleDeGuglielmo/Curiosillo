package com.example.curiosillo.domain

/**
 * Result of the password validation process.
 */
sealed class PasswordValidationResult {
    object Valid : PasswordValidationResult()
    object TooShort : PasswordValidationResult()
    object NoUppercase : PasswordValidationResult()
    object NoNumber : PasswordValidationResult()
    object Empty : PasswordValidationResult()
}

/**
 * Business logic for password validation.
 * Rules:
 * - Minimum 8 characters
 * - At least one uppercase letter
 * - At least one number
 */
object PasswordValidator {

    fun validate(password: String): PasswordValidationResult {
        if (password.isEmpty()) {
            return PasswordValidationResult.Empty
        }

        if (password.length < 8) {
            return PasswordValidationResult.TooShort
        }

        if (!password.any { it.isUpperCase() }) {
            return PasswordValidationResult.NoUppercase
        }

        if (!password.any { it.isDigit() }) {
            return PasswordValidationResult.NoNumber
        }

        return PasswordValidationResult.Valid
    }
}
