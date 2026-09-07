package com.example.urlshortener.domain;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class RandomBase62ShortCodeGeneratorTest {
    @Test
    void generatesEightBase62Characters() {
        var generator = new RandomBase62ShortCodeGenerator(new Random(1), 8);
        assertThat(generator.generate()).matches("[0-9A-Za-z]{8}");
    }

    @Test
    void consecutiveValuesDiffer() {
        var generator = new RandomBase62ShortCodeGenerator(new Random(2), 8);
        assertThat(generator.generate()).isNotEqualTo(generator.generate());
    }
}
