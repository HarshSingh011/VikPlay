package com.example.vidplay.util

object PasswordValidator {
    
    

    fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false
        
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { it in "@\$!%*?&" }
        
        return hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar
    }

    

    fun getPasswordErrorMessage(password: String): String {
        val errors = mutableListOf<String>()
        
        if (password.length < 8) {
            errors.add("at least 8 characters")
        }
        if (!password.any { it.isUpperCase() }) {
            errors.add("uppercase letter")
        }
        if (!password.any { it.isLowerCase() }) {
            errors.add("lowercase letter")
        }
        if (!password.any { it.isDigit() }) {
            errors.add("digit")
        }
        if (!password.any { it in "@\$!%*?&" }) {
            errors.add("special character (@\$!%*?&)")
        }
        
        return "Password must contain: ${errors.joinToString(", ")}"
    }
}
