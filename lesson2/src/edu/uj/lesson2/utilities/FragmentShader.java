package edu.uj.lesson2.utilities;

import android.opengl.GLES20;

/**
 * Created by shybovycha on 16.12.14.
 */
public class FragmentShader extends Shader {
    public FragmentShader(String source) {
        super(GLES20.GL_FRAGMENT_SHADER, source);
    }
}
