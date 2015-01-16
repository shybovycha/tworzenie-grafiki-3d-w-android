package edu.uj.lesson2.utilities;

import android.opengl.GLES20;

/**
 * Created by shybovycha on 16.12.14.
 */
public class Shader {
    protected String source;
    protected Integer type;
    protected Integer handle;

    public Shader(int type, String source) {
        this.type = type;
        this.source = source;
    }

    public int compile() {
        // if (this.type != GLES20.GL_VERTEX_SHADER && this.type != GLES20.GL_FRAGMENT_SHADER) {
        //   throw new Exception("Shader is not valid");
        // }

        if (this.handle != null) {
            return this.handle;
        }

        this.handle = GLES20.glCreateShader(this.type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(this.handle, this.source);
        GLES20.glCompileShader(this.handle);

        return this.handle;
    }
}
