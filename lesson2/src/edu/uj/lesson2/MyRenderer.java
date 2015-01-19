package edu.uj.lesson2;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;
import edu.uj.lesson2.utilities.*;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

public class MyRenderer implements GLSurfaceView.Renderer {
    /**
     * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
     * of being located at the center of the universe) to world space.
     */
    private float[] mModelMatrix = new float[16];

    /**
     * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
     * it positions things relative to our eye.
     */
    private float[] mViewMatrix = new float[16];

    /**
     * Store the projection matrix. This is used to project the scene onto a 2D viewport.
     */
    private float[] mProjectionMatrix = new float[16];

    /**
     * Allocate storage for the final combined matrix. This will be passed into the shader program.
     */
    private float[] mMVPMatrix = new float[16];

    private Camera mCamera;

    /**
     * This will be used to pass in the transformation matrix.
     */
    private int mMVPMatrixHandle;

    /**
     * This will be used to pass in model position information.
     */
    private int mPositionHandle;

    private MD2Model mModel;
    private int frame = 0;
    private Context mContext;
    private Triangle mTriangle;
    private FloatBuffer mTriangleVertexBuffer;

    /**
     * Initialize the model data.
     */
    public MyRenderer(Context context) {
        mContext = context;
    }

    protected RandomAccessFile toRandomAccessFile(InputStream inputStream) throws IOException {
        int fileSize = inputStream.available();
        File tempFile = File.createTempFile("tmp_md2_bin", null, mContext.getCacheDir());

        RandomAccessFile randomAccessFile = new RandomAccessFile(tempFile, "rw");

        byte[] buffer = new byte[fileSize];
        int numBytesRead = 0;

        while ( (numBytesRead = inputStream.read(buffer)) != -1 ) {
            randomAccessFile.write(buffer, 0, numBytesRead);
        }

        randomAccessFile.seek(0);

        return randomAccessFile;
    }

    public void handleSwipe(float dx, float dy) {
        Vector3d camPos = mCamera.getEyePos();

        camPos.setX(camPos.getX() + dx);
        camPos.setZ(camPos.getZ() + dy);
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        // Set the background clear color to gray.
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 0.5f);

        float triangleCoords[] = {   // in counterclockwise order:
                -0.5f, -0.25f, 0.0f,
                0.5f, -0.25f, 0.0f,
                0.0f, 0.5f, 0.0f
        };

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                triangleCoords.length * 4);

        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());

        // create a floating point buffer from the ByteBuffer
        mTriangleVertexBuffer = bb.asFloatBuffer();

        // add the coordinates to the FloatBuffer
        mTriangleVertexBuffer.put(triangleCoords);

        // set the buffer to read the first coordinate
        mTriangleVertexBuffer.position(0);

        /*ShaderProgram identityProgram = new ShaderProgram();
        identityProgram.attach(new IdentityFragmentShader());
        identityProgram.attach(new IdentityVertexShader());
        identityProgram.use();

        mPositionHandle = GLES20.glGetAttribLocation(identityProgram.getHandle(), "vPosition");
        mMVPMatrixHandle = GLES20.glGetAttribLocation(identityProgram.getHandle(), "u_MVPMatrix");*/

        final String vertexShader =
                "uniform mat4 u_MVPMatrix;      \n"    // A constant representing the combined model/view/projection matrix.

                        + "attribute vec4 a_Position;     \n"    // Per-vertex position information we will pass in.
                        + "attribute vec4 a_Color;        \n"    // Per-vertex color information we will pass in.

                        + "varying vec4 v_Color;          \n"    // This will be passed into the fragment shader.

                        + "void main()                    \n"    // The entry point for our vertex shader.
                        + "{                              \n"
                        + "   v_Color = a_Color;          \n"    // Pass the color through to the fragment shader.
                        // It will be interpolated across the triangle.
                        + "   gl_Position = u_MVPMatrix   \n"   // gl_Position is a special variable used to store the final position.
                        + "               * a_Position;   \n"     // Multiply the vertex by the matrix to get the final point in
                        + "}                              \n";    // normalized screen coordinates.

        final String fragmentShader =
                "precision mediump float;       \n"    // Set the default precision to medium. We don't need as high of a
                        // precision in the fragment shader.
                        + "varying vec4 v_Color;          \n"    // This is the color from the vertex shader interpolated across the
                        // triangle per fragment.
                        + "void main()                    \n"    // The entry point for our fragment shader.
                        + "{                              \n"
                        + "   gl_FragColor = v_Color;     \n"    // Pass the color directly through the pipeline.
                        + "}                              \n";

        // Load in the vertex shader.
        int vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);

        if (vertexShaderHandle != 0) {
            // Pass in the shader source.
            GLES20.glShaderSource(vertexShaderHandle, vertexShader);

            // Compile the shader.
            GLES20.glCompileShader(vertexShaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(vertexShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                GLES20.glDeleteShader(vertexShaderHandle);
                vertexShaderHandle = 0;
            }
        }

        if (vertexShaderHandle == 0) {
            throw new RuntimeException("Error creating vertex shader.");
        }

        // Load in the fragment shader shader.
        int fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);

        if (fragmentShaderHandle != 0) {
            // Pass in the shader source.
            GLES20.glShaderSource(fragmentShaderHandle, fragmentShader);

            // Compile the shader.
            GLES20.glCompileShader(fragmentShaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(fragmentShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                GLES20.glDeleteShader(fragmentShaderHandle);
                fragmentShaderHandle = 0;
            }
        }

        if (fragmentShaderHandle == 0) {
            throw new RuntimeException("Error creating fragment shader.");
        }

        // Create a program object and store the handle to it.
        int programHandle = GLES20.glCreateProgram();

        if (programHandle != 0) {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, vertexShaderHandle);

            // Bind the fragment shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);

            // Bind attributes
            GLES20.glBindAttribLocation(programHandle, 0, "a_Position");
            GLES20.glBindAttribLocation(programHandle, 1, "a_Color");

            // Link the two shaders together into a program.
            GLES20.glLinkProgram(programHandle);

            // Get the link status.
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

            // If the link failed, delete the program.
            if (linkStatus[0] == 0) {
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        if (programHandle == 0) {
            throw new RuntimeException("Error creating program.");
        }

        mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position");

        mModel = new MD2Model(mPositionHandle, mMVPMatrixHandle);

        try {
            RandomAccessFile md2File = toRandomAccessFile(mContext.getAssets().open("model.md2"));
            mModel.Load(md2File);

            Log.d("MD2 BBOX", String.format("%.3f x %.3f x %.3f", mModel.getWidth(), mModel.getHeight(), mModel.getDepth()));
        } catch (IOException e) {
            Log.d("MD2", e.getMessage());
        }

        GLES20.glUseProgram(programHandle);
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float ratio = (float) width / height;
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 1.0f;
        final float far = 1000.0f;

        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        // Do a complete rotation every 10 seconds.
        long time = SystemClock.uptimeMillis() % 1000L;
        float angleInDegrees = (360.0f / 1000.0f) * ((int) time);
        frame = (int) (mModel.getFrameCount() / 500.0f) * ((int) time);

        // Set the camera position (View matrix)
//        mCamera.project(mViewMatrix);

        // hack for this MD2 model
        Matrix.setLookAtM(mViewMatrix, 0, 100, 50, 50, 0, 0, 0, 0, 0, 1);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        Matrix.setIdentityM(mModelMatrix, 0);
//        Matrix.rotateM(mModelMatrix, 0, 90, 0.0f, 0.0f, 1.0f);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        mModel.Draw(frame, mMVPMatrix, mModelMatrix, mViewMatrix, mProjectionMatrix);
        // mTriangle.draw(mMVPMatrix, mModelMatrix, mViewMatrix, mProjectionMatrix);

        // Prepare the triangle coordinate data
        /*mTriangleVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, 3,
                GLES20.GL_FLOAT, false,
                12, mTriangleVertexBuffer);

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);*/
    }
}