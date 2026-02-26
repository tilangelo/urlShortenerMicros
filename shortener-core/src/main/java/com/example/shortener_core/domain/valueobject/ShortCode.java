package com.example.shortener_core.domain.valueobject;


public class ShortCode {
    private final String value;

    private ShortCode(String value) {
        if(value == null || value.trim().isEmpty()){
            throw new IllegalArgumentException("Short code cannot be empty");
        } else if (!value.matches("^[a-zA-Z0-9]+$")) {
            throw new IllegalArgumentException("short code should be alphanumeric");
        }

        this.value = value;
    }


    public static ShortCode of(String value) {
        return new ShortCode(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShortCode shortCode = (ShortCode) o;
        return value.equals(shortCode.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
