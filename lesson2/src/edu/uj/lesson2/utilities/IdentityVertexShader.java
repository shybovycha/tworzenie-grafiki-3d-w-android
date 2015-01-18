package edu.uj.lesson2.utilities;

/**
 * Created by shybovycha on 16.12.14.
 */
public class IdentityVertexShader extends VertexShader {
    protected static String source = "attribute vec4 vPosition;\nuniform mat4 u_MVPMatrix;\nvoid main() {\ngl_Position = vPosition;\n}";

    public IdentityVertexShader() {
        super(source);
    }
}
