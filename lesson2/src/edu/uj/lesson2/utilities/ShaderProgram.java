package edu.uj.lesson2.utilities;

import android.opengl.GLES20;

import java.util.Vector;

/**
 * Created by shybovycha on 16.12.14.
 */
public class ShaderProgram {
    protected Vector<Shader> shaders;
    protected Integer handle;

    public ShaderProgram() {
        this.shaders = new Vector<Shader>();
    }

    public void attach(Shader s) {
        this.shaders.add(s);
    }

    public int link() {
        if (this.handle != null) {
            return this.handle;
        }

        this.handle = GLES20.glCreateProgram();

        for (Shader shader : this.shaders) {
            GLES20.glAttachShader(this.handle, shader.compile());
        }

        GLES20.glLinkProgram(this.handle);

        return this.handle;
    }

    public int getHandle() {
        return this.link();
    }

    public void use() {
        GLES20.glUseProgram(this.link());
    }
}
