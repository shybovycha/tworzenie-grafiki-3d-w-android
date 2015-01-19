package edu.uj.lesson2.utilities;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by shybovycha on 16.12.14.
 */
public class Triangle {
    private FloatBuffer vertexBuffer;

    static final int COORDS_PER_VERTEX = 3;
    static final int BYTES_PER_FLOAT = 4;

    static float triangleCoords[] = {   // in counterclockwise order:
            -0.5f, -0.25f, 0.0f,
            0.5f, -0.25f, 0.0f,
            0.0f, 0.5f, 0.0f
    };

    private int mMVPMatrixHandle;
    private int mPositionHandle;
    private ShaderProgram identityProgram;

    static final int strideBytes = COORDS_PER_VERTEX * BYTES_PER_FLOAT;

    public Triangle() {
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                triangleCoords.length * BYTES_PER_FLOAT);

        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());

        // create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer();

        // add the coordinates to the FloatBuffer
        vertexBuffer.put(triangleCoords);

        // set the buffer to read the first coordinate
        vertexBuffer.position(0);

        identityProgram = new ShaderProgram();
        identityProgram.attach(new IdentityFragmentShader());
        identityProgram.attach(new IdentityVertexShader());
        identityProgram.use();

        mPositionHandle = GLES20.glGetAttribLocation(identityProgram.getHandle(), "vPosition");
        mMVPMatrixHandle = GLES20.glGetAttribLocation(identityProgram.getHandle(), "u_MVPMatrix");
    }

    public void draw(float[] mMVPMatrix, float[] mModelMatrix, float[] mViewMatrix, float[] mProjectionMatrix) {
        vertexBuffer.position(0);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                strideBytes, vertexBuffer);

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, triangleCoords.length / COORDS_PER_VERTEX);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}
