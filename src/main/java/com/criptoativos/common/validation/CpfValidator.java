package com.criptoativos.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<Cpf, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // absence is @NotNull's job, not ours
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() != 11) {
            return false;
        }
        // Sequences like 111.111.111-11 satisfy the checksum but are never issued.
        if (digits.chars().distinct().count() == 1) {
            return false;
        }
        return checkDigit(digits, 9) == digits.charAt(9) && checkDigit(digits, 10) == digits.charAt(10);
    }

    /** Computes the CPF check digit at {@code position} using the standard modulus-11 weighting. */
    private static char checkDigit(String digits, int position) {
        int sum = 0;
        int weight = position + 1;
        for (int i = 0; i < position; i++) {
            sum += (digits.charAt(i) - '0') * weight--;
        }
        int remainder = sum % 11;
        return (char) ('0' + (remainder < 2 ? 0 : 11 - remainder));
    }
}
