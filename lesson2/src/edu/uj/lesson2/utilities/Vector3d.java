package edu.uj.lesson2.utilities;

/**
 * Created by shybovycha on 16.12.14.
 */
public class Vector3d {
    protected float x;
    protected float y;
    protected float z;

    public Vector3d(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3d add(Vector3d that) {
        return new Vector3d(this.x + that.x, this.y + that.y, this.z + that.z);
    }

    public Vector3d multiply(float v) {
        return new Vector3d(this.x * v, this.y * v, this.z * v);
    }

    public float length() {
        return (float) Math.sqrt((this.x * this.x) + (this.y * this.y) + (this.z * this.z));
    }

    public Vector3d normalize() {
        float l = this.length();

        return new Vector3d(this.x / l, this.y / l, this.z / l);
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }
}
