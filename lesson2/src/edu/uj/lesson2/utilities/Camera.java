package edu.uj.lesson2.utilities;

import android.opengl.Matrix;
import android.util.Log;

/**
 * Created by shybovycha on 16.12.14.
 */
public class Camera {
    protected Vector3d eyePos;
    protected Vector3d up;
    protected Vector3d lookAt;

    public Camera() {
        eyePos = new Vector3d(0, 0, 0);
        up = new Vector3d(0, 1, 0);
        lookAt = new Vector3d(0, 0, -1);
    }

    public Camera(Vector3d eyePos, Vector3d up, Vector3d lookAt) {
        this.eyePos = eyePos;
        this.up = up;
        this.lookAt = lookAt;
    }

    public void project(float[] viewMatrix) {
        Matrix.setLookAtM(viewMatrix, 0, eyePos.x, eyePos.y, eyePos.z, lookAt.x, lookAt.y, lookAt.z, up.x, up.y, up.z);
    }

    public Vector3d getEyePos() {
        return eyePos;
    }

    public void setEyePos(Vector3d eyePos) {
        this.eyePos = eyePos;
    }

    public Vector3d getUp() {
        return up;
    }

    public void setUp(Vector3d up) {
        this.up = up;
    }

    public Vector3d getLookAt() {
        return lookAt;
    }

    public void setLookAt(Vector3d lookAt) {
        this.lookAt = lookAt;
    }
}
