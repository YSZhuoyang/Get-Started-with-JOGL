import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;


/** 
 * 
 * @author gbarbieri 
 */ 
public class GLSLTextureSample implements GLEventListener { 

    private int imageWidth = 800;
    private int imageHeight = 600;
    private GLCanvas canvas;
    private int[] vertexBufferObject = new int[1];
    private int[] vertexArrayObject = new int[1];
    private float[] vertexData = new float[]{ 
        0.25f, 0.25f, 0.75f, 1.0f, 
        0.25f, -0.25f, 0.75f, 1.0f, 
        -0.25f, -0.25f, 0.75f, 1.0f, 
        -0.25f, 0.25f, 0.75f, 1.0f, 
        1.0f, 1.0f, 
        1.0f, 0.0f, 
        0.0f, 0.0f, 
        0.0f, 1.0f}; 
    //private String shadersFilepath = "/tut04/shaders/"; 
    private Texture texture; 
    private int textureUnLoc;
	private int shaderProgram; 
	private BufferedImage image;
	
    /** 
     * @param args the command line arguments 
     */ 
    public static void main(String[] args) { 
    	GLSLTextureSample test = new GLSLTextureSample(); 

        Frame frame = new Frame("Test"); 

        frame.add(test.getCanvas()); 

        frame.setSize(test.getCanvas().getWidth(), test.getCanvas().getHeight()); 

        frame.addWindowListener(new WindowAdapter() { 
            @Override 
            public void windowClosing(WindowEvent windowEvent) { 
                System.exit(0); 
            } 
        }); 

        frame.setVisible(true); 
    } 

    public GLSLTextureSample() { 
        initGL(); 
    } 

    private void initGL() { 
        GLProfile profile = GLProfile.getDefault(); 

        GLCapabilities capabilities = new GLCapabilities(profile); 

        canvas = new GLCanvas(capabilities); 

        canvas.setSize(imageWidth, imageHeight); 

        canvas.addGLEventListener(this); 
    } 

    @Override 
    public void init(GLAutoDrawable glad) { 
        System.out.println("init"); 

        canvas.setAutoSwapBufferMode(false); 

        GL3 gl3 = glad.getGL().getGL3(); 

        buildShaders(gl3); 

        initializeVertexBuffer(gl3); 

        texture = initializeTexture(gl3); 

        gl3.glGenVertexArrays(1, IntBuffer.wrap(vertexArrayObject)); 
        gl3.glBindVertexArray(vertexArrayObject[0]); 

        gl3.glEnable(GL3.GL_CULL_FACE); 
        gl3.glCullFace(GL3.GL_BACK); 
        gl3.glFrontFace(GL3.GL_CW); 
    } 

    @Override 
    public void dispose(GLAutoDrawable glad) { 
        System.out.println("dispose"); 
    } 

    @Override 
    public void display(GLAutoDrawable glad) { 
        System.out.println("display"); 

        GL3 gl3 = glad.getGL().getGL3(); 

        gl3.glClearColor(0.0f, 0.0f, 0.0f, 1.0f); 
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT); 
        gl3.glUseProgram(shaderProgram);
        //programObject.bind(gl3); 
        { 
            gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, vertexBufferObject[0]); 

            gl3.glEnableVertexAttribArray(0); 
            gl3.glEnableVertexAttribArray(1); 
            { 
                gl3.glActiveTexture(GL3.GL_TEXTURE0); 
                texture.enable(gl3); 
                texture.bind(gl3); 
                gl3.glUniform1i(textureUnLoc, 0); 
                
                gl3.glVertexAttribPointer(0, 4, GL3.GL_FLOAT, false, 0, 0); 
                gl3.glVertexAttribPointer(1, 2, GL3.GL_FLOAT, false, 0, 4 * 4 * 4); 

                gl3.glDrawArrays(GL3.GL_QUADS, 0, 4); 
                
                texture.disable(gl3); 
            } 
            gl3.glDisableVertexAttribArray(0); 
            gl3.glDisableVertexAttribArray(1); 
        } 
        //programObject.unbind(gl3); 

        glad.swapBuffers(); 
    } 

    @Override 
    public void reshape(GLAutoDrawable glad, int x, int y, int w, int h) { 
        System.out.println("reshape() x: " + x + " y: " + y + " width: " + w + " height: " + h); 

        GL3 gl3 = glad.getGL().getGL3(); 

        gl3.glViewport(x, y, w, h); 
    } 

    private void buildShaders(GL3 gl) { 
        System.out.print("Building shaders..."); 

        //programObject = new GLSLProgramObject(gl3); 
        //programObject.attachVertexShader(gl3, shadersFilepath + "OrthoWithOffset_VS.glsl"); 
        //programObject.attachFragmentShader(gl3, shadersFilepath + "StandardColor_FS.glsl"); 
        //programObject.initializeProgram(gl3, true); 

        textureUnLoc = gl.glGetUniformLocation(shaderProgram, "myTexture"); 

        System.out.println("ok"); 
        
        int verShader = gl.glCreateShader(GL4.GL_VERTEX_SHADER); 
 	   int fraShader = gl.glCreateShader(GL4.GL_FRAGMENT_SHADER); 
 	   
 	   String[] vsrc = 
                     { 
                             "#version 430 core									\n" + 
                             "layout (location = 1) in vec2 vertexUV;			\n" + 
                             "layout (location = 0) in vec4 position;			\n" + 
                             "out vec2 fragmentUV;					\n" + 
                             "void main(void) 									\n" + 
                             "{        											\n" + 
                             "	gl_Position = position;							\n" + 
                             "	fragmentUV = vertexUV;							\n" + 
                             "}"
                     }; 

 	   gl.glShaderSource(verShader, 1, vsrc, null); 
 	   gl.glCompileShader(verShader); 
 	   //printShaderInfoLog(gl, verShader);
 	   
 	   String[] fsrc = 
                     { 
                             "#version 430 core						\n" + 
                             "layout (binding = 0) uniform sampler2D myTexture;	\n" + 
                             "in vec2 fragmentUV;							\n" + 
                             "out vec4 outputColor;						\n" + 
                             "void main(void)						\n" + 
                             "{										\n" + 
                             "	outputColor = texture(myTexture, fragmentUV).rgba;	\n" + 
                             "}"
                     };

 	   gl.glShaderSource(fraShader, 1, fsrc, null); 
 	   gl.glCompileShader(fraShader); 
 	   //printShaderInfoLog(gl, fraShader);
 	   
 	   shaderProgram = gl.glCreateProgram(); 
 	   gl.glAttachShader(shaderProgram, verShader); 
 	   gl.glAttachShader(shaderProgram, fraShader); 
 	   gl.glLinkProgram(shaderProgram); 
 	   gl.glValidateProgram(shaderProgram); 
 	   //printProgramInfoLog(gl, shaderProgram);
 	   
 	   //gl.glGenVertexArrays(1, vaoNameBuff); 
 	   //gl.glBindVertexArray(vaoNameBuff.get(0)); 
    } 

    private void initializeVertexBuffer(GL3 gl3) { 
        gl3.glGenBuffers(1, IntBuffer.wrap(vertexBufferObject)); 

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, vertexBufferObject[0]); 
        { 
            FloatBuffer buffer = GLBuffers.newDirectFloatBuffer(vertexData); 

            gl3.glBufferData(GL3.GL_ARRAY_BUFFER, vertexData.length * 4, buffer, GL3.GL_STATIC_DRAW); 
        } 
        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0); 
    } 

    private Texture initializeTexture(GL3 gl3) { 

        Texture t = null; 

        try { 
            //t = TextureIO.newTexture(this.getClass().getResource("data/Texture.jpg"), false, ".jpg"); 
            image = ImageIO.read(new File("images/tree.png"));
	    	t = AWTTextureIO.newTexture(GLProfile.getDefault(), image, false);
	    	
            t.setTexParameteri(gl3, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR); 
            t.setTexParameteri(gl3, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR); 
            t.setTexParameteri(gl3, GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE); 
            t.setTexParameteri(gl3, GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE); 

        } catch (IOException | GLException ex) { 
            Logger.getLogger(GLSLTextureSample.class.getName()).log(Level.SEVERE, null, ex); 
        } 

        return t; 
    } 

    public GLCanvas getCanvas() { 
        return canvas; 
    } 

    public void setCanvas(GLCanvas canvas) { 
        this.canvas = canvas; 
    } 
}