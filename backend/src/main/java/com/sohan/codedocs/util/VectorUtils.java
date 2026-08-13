package com.sohan.codedocs.util;

public final class VectorUtils {

    private VectorUtils() {
        throw new AssertionError("utility class");
    }

    /**
     * gemini-embedding-001 pre-normalises only its full 3072-dimension output.
     * At any reduced dimensionality the vectors arrive unnormalised, and cosine
     * distance then ranks partly by magnitude instead of purely by direction.
     * Skipping this produces confidently wrong retrieval with no error anywhere.
     */
    public static float[] normalise(float[] vector) {
        if (vector == null || vector.length == 0) {
            throw new IllegalArgumentException("vector must not be empty");
        }
        double sumOfSquares = 0d;
        for (float v : vector) {
            sumOfSquares += (double) v * v;
        }
        double norm = Math.sqrt(sumOfSquares);
        if (norm == 0d) {
            return vector.clone();
        }
        float[] result = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            result[i] = (float) (vector[i] / norm);
        }
        return result;
    }

    /** pgvector literal form: [0.013,-0.245,...] */
    public static String toPgVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 12 + 2).append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
