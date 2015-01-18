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
    private FloatBuffer mTriangle1Vertices;

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

        mCamera = new Camera();
        mCamera.setEyePos(new Vector3d(0, 0, 1.5f));
        mCamera.setLookAt(new Vector3d(0, 0, -5));

        Shader vertexShader = new VertexShader(
                "uniform mat4 u_MVPMatrix;      \n"    // A constant representing the combined model/view/projection matrix.
                        + "attribute vec4 a_Position;     \n"    // Per-vertex position information we will pass in.
                        + "void main()                    \n"    // The entry point for our vertex shader.
                        + "{                              \n"
                        + "   gl_Position = u_MVPMatrix   \n"   // gl_Position is a special variable used to store the final position.
                        + "               * a_Position;   \n"     // Multiply the vertex by the matrix to get the final point in
                        + "}                              \n"    // normalized screen coordinates.
        );

        Shader fragmentShader = new IdentityFragmentShader();

        ShaderProgram shaderProgram = new ShaderProgram();
        shaderProgram.attach(vertexShader);
        shaderProgram.attach(fragmentShader);

        int programHandle = shaderProgram.link();

        // Set program handles. These will later be used to pass in values to the program.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position");

        // Tell OpenGL to use this program when rendering.
        shaderProgram.use();

        /*mModel = new MD2Model(mMVPMatrixHandle, mPositionHandle);
        try {
            RandomAccessFile md2File = toRandomAccessFile(mContext.getAssets().open("eagle.md2"));
            mModel.Load(md2File);

            Log.d("MD2 BBOX", String.format("%.3f x %.3f x %.3f", mModel.getWidth(), mModel.getHeight(), mModel.getDepth()));
        } catch (IOException e) {
            Log.d("MD2", e.getMessage());
        }*/

        mTriangle = new Triangle();
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
        long time = SystemClock.uptimeMillis() % 10000L;
        float angleInDegrees = (360.0f / 10000.0f) * ((int) time);
        // frame = (frame + 1) % mModel.getFrameCount();

        // mModel.Draw(frame);

        // Set the camera position (View matrix)
        mCamera.project(mViewMatrix);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 0.0f, 1.0f);

        mTriangle.draw(mMVPMatrix, mModelMatrix, mViewMatrix, mProjectionMatrix);
    }
}