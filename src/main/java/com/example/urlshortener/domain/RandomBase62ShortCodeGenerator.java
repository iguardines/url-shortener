package com.example.urlshortener.domain;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

public final class RandomBase62ShortCodeGenerator implements ShortCodeGenerator {
    static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private final RandomGenerator random;
    private final int length;

    public RandomBase62ShortCodeGenerator() {
        this(new SecureRandom(), 8);
    }

    RandomBase62ShortCodeGenerator(RandomGenerator random, int length) {
        this.random = random;
        this.length = length;
    }

    @Override
    public String generate() {
        var value = new StringBuilder(length);
        for (int i = 0; i < length; i++) value.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return value.toString();
    }
}
