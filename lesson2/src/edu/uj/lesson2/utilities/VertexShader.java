package edu.uj.lesson2.utilities;

import android.opengl.GLES20;

/**
 * Created by shybovycha on 16.12.14.
 */
public class VertexShader extends Shader {
    public VertexShader(String source) {
        super(GLES20.GL_VERTEX_SHADER, source);
    }
}
