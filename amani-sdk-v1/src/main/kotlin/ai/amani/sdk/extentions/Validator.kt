package ai.amani.sdk.extentions

import android.util.Patterns
import android.widget.EditText
import java.util.regex.Pattern

class Validator {
    companion object {

        /**
         * Checks if the email is valid.
         * @return - true if the email is valid.
         */
        fun EditText.isValidEmail(): Boolean {
            return Patterns.EMAIL_ADDRESS.matcher(this.text).matches()
        }

        /**
         * Checks if the email is valid.
         * @return - true if the email is valid.
         */
        fun String?.isValidEmail(): Boolean {
            if (this == null) return false
            return Patterns.EMAIL_ADDRESS.matcher(this).matches()
        }

        /**
         * Checks if the phone is valid.
         * @return - true if the phone is valid.
         */
        fun String.isValidPhone(): Boolean {
            return Patterns.PHONE.matcher(this).matches()
        }

        /**
         * Checks if the password is valid as per the following password policy.
         * Password should be minimum minimum 8 characters long.
         * Password should contain at least one number.
         * Password should contain at least one capital letter.
         * Password should contain at least one small letter.
         * Password should contain at least one special character.
         * Allowed special characters: "~!@#$%^&*()-_=+|/,."';:{}[]<>?"
         *
         * @return - true if the password is valid as per the password policy.
         */
        fun EditText.isValidPassword(): Boolean {

            // Rule 1: Minimum 8 characters
            if (this.text.length < 8) {
                return false
            }

            // Rule 2: Upper and lower case characters
            val hasUpperCase = this.text.any { it.isUpperCase() }
            val hasLowerCase = this.text.any { it.isLowerCase() }

            if (!hasUpperCase || !hasLowerCase) {
                return false
            }

            // Rule 3: At least one symbol
            val hasSymbol = this.text.any { it.isLetterOrDigit().not() }

            return hasSymbol
        }

        fun String.isValidPassword(): Boolean {

            // Rule 1: Minimum 8 characters
            if (this.length < 8) {
                return false
            }

            // Rule 2: Upper and lower case characters
            val hasUpperCase = this.any { it.isUpperCase() }
            val hasLowerCase = this.any { it.isLowerCase() }

            if (!hasUpperCase || !hasLowerCase) {
                return false
            }

            // Rule 3: At least one symbol
            val hasSymbol = this.any { it.isLetterOrDigit().not() }

            return hasSymbol
        }

    }
}