package org.firstinspires.ftc.teamcode.Swerve.Geometry;

/**
 * Vector: A general-purpose N-dimensional vector.
 * The unified math engine for all swerve logic.
 */
public class Vector {
    public final double[] components;
    public final int dimension;

    public Vector(double... components) {
        this.components = components;
        this.dimension = components.length;
    }

    public double get(int i) {
        return components[i];
    }

    public Vector add(Vector other) {
        if (this.dimension != other.dimension) throw new IllegalArgumentException("Dimension mismatch");
        double[] result = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            result[i] = this.components[i] + other.components[i];
        }
        return new Vector(result);
    }

    public Vector subtract(Vector other) {
        if (this.dimension != other.dimension) throw new IllegalArgumentException("Dimension mismatch");
        double[] result = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            result[i] = this.components[i] - other.components[i];
        }
        return new Vector(result);
    }

    public Vector scale(double scalar) {
        double[] result = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            result[i] = this.components[i] * scalar;
        }
        return new Vector(result);
    }

    /**
     * Linearly interpolates between this vector and another.
     * Useful for Low-Pass Filtering: current.lerp(target, alpha)
     */
    public Vector lerp(Vector other, double alpha) {
        if (this.dimension != other.dimension) throw new IllegalArgumentException("Dimension mismatch");
        double[] result = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            result[i] = this.components[i] + alpha * (other.components[i] - this.components[i]);
        }
        return new Vector(result);
    }

    public double dot(Vector other) {
        if (this.dimension != other.dimension) throw new IllegalArgumentException("Dimension mismatch");
        double sum = 0;
        for (int i = 0; i < dimension; i++) {
            sum += this.components[i] * other.components[i];
        }
        return sum;
    }

    public double magnitude() {
        double sum = 0;
        for (double c : components) {
            sum += c * c;
        }
        return Math.sqrt(sum);
    }

    /**
     * Rotates the vector.
     * For 2D vectors, assumes rotation in the XY plane.
     * For 3D+ vectors, requires an axis index (0=X, 1=Y, 2=Z).
     */
    public Vector rotate(double angle) {
        if (dimension == 2) return rotate(angle, 2); 
        throw new IllegalArgumentException("For 3D+ vectors, specify rotation axis (0=X, 1=Y, 2=Z)");
    }

    public Vector rotate(double angle, int axisIndex) {
        if (dimension < 2) throw new IllegalStateException("Rotation requires at least 2 dimensions");
        
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        double[] result = components.clone();

        if (dimension == 2 || axisIndex == 2) { // Rotate around Z
            result[0] = components[0] * cos - components[1] * sin;
            result[1] = components[0] * sin + components[1] * cos;
        } else if (axisIndex == 0) { // Rotate around X
            result[1] = components[1] * cos - components[2] * sin;
            result[2] = components[1] * sin + components[2] * cos;
        } else if (axisIndex == 1) { // Rotate around Y
            result[0] = components[0] * cos + components[2] * sin;
            result[2] = -components[0] * sin + components[2] * cos;
        }

        return new Vector(result);
    }

    /** 2D Cross Product: returns scalar magnitude */
    public double cross(Vector other) {
        if (this.dimension != 2 || other.dimension != 2) 
            throw new IllegalStateException("2D Cross product requires 2D vectors");
        return this.get(0) * other.get(1) - this.get(1) * other.get(0);
    }

    /** Helper for X component */
    public double x() { return components[0]; }
    /** Helper for Y component */
    public double y() { return components[1]; }
    /** Helper for Z / Omega component */
    public double z() { return components[2]; }
    public double omega() { return components[2]; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Vector[");
        for (int i = 0; i < dimension; i++) {
            sb.append(String.format("%.3f", components[i]));
            if (i < dimension - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
