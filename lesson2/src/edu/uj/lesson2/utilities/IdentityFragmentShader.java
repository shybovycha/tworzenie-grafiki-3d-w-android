package edu.uj.lesson2.utilities;

/**
 * Created by shybovycha on 16.12.14.
 */
public class IdentityFragmentShader extends FragmentShader {
    protected static String source = "precision mediump float;\n\nvoid main() {\ngl_FragColor[0] = 0.0; gl_FragColor[1] = 0.0; gl_FragColor[1] = 1.0;\n}";

    public IdentityFragmentShader() {
        super(source);
    }
}
