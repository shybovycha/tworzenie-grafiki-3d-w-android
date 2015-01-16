package edu.uj.lesson2.utilities;

/**
 * Created by shybovycha on 16.12.14.
 */
public class IdentityFragmentShader extends FragmentShader {
    protected static String source = "precision mediump float;\nuniform vec4 vColor;\nvoid main() {\ngl_FragColor = vColor;\n}";

    public IdentityFragmentShader() {
        super(source);
    }
}
