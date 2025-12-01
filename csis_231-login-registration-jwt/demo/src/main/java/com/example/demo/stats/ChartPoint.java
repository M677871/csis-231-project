package com.example.demo.stats;

/**
 * DTO representing a single chart point returned by statistics endpoints.
 *
 * @param label display label (e.g., quiz name)
 * @param value numeric value (percentage or count)
 */
public record ChartPoint(String label, double value) { }
