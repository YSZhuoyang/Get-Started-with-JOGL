import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;


/**
 * @author Yu Sangzhuoyang
 * @version 3.02
 */
public class GLSLMovingTexturedRect extends GLCanvas implements GLEventListener 
{
	private static final long serialVersionUID = 1L;
	
	// Define constants for the top-level container
	private static String TITLE = "Moving textured rectangle";
	private static final int CANVAS_WIDTH = 320;  // width of the drawable
	private static final int CANVAS_HEIGHT = 240; // height of the drawable
	private static final int FPS = 60; // animator's target frames per second
	
	private IntBuffer vaoNameBuff = GLBuffers.newDirectIntBuffer(1);
	private IntBuffer vboNameBuff = GLBuffers.newDirectIntBuffer(1);
	private FloatBuffer vertBuff = GLBuffers.newDirectFloatBuffer(16);
	
	private Texture tex;
	private BufferedImage image;
	
	private int shaderProgram;
	
	private int verPosLoc;
	private int offsetLoc;
	private static int tc_inLoc;
	
	private float green[] = { 0.0f, 0.25f, 0.0f, 1.0f };
	
	private float temp;
	
	private static Texture tex2;
	private static BufferedImage image2;
	public static IntBuffer tbo = GLBuffers.newDirectIntBuffer(1);
	public static FloatBuffer texBuff;
	
	public GLSLMovingTexturedRect()
	{
		addGLEventListener(this);
	}
	
	public void initShaders(GL4 gl)
	{
		int verShader = gl.glCreateShader(GL4.GL_VERTEX_SHADER);
		int fraShader = gl.glCreateShader(GL4.GL_FRAGMENT_SHADER);
		
		String[] vsrc = 
			{
				"#version 430							\n" + 
				"layout (location = 0) in vec4 offset;	\n" + 
				"layout (location = 1) in vec4 position;\n" + 
				"layout (location = 2) in vec2 tc_in;	\n" + 
				//notice that the array variable should not use qualifiers like 
				//"layout (location = 0)", otherwise it will be regarded as a single 
				//element of that array.
				"out vec2 tc;							\n" + 
				"void main(void)						\n" + 
				"{										\n" + 
				"	gl_Position = position + offset;	\n" + 
				"	tc = tc_in;							\n" + 
				"}"
			};System.out.println(vsrc[0]);
		
		gl.glShaderSource(verShader, 1, vsrc, null);
		gl.glCompileShader(verShader);
		printShaderInfoLog(gl, verShader);
		
		String[] fsrc = 
			{
				"#version 430			\n" + 
				// Output to the framebuffer
				"layout (binding = 0) uniform sampler2D tex_object;	\n" + 
                "in vec2 tc;			\n" + 
				"out vec4 color;		\n" + 
				"void main(void)		\n" + 
				"{						\n" + 
				"	color = texture(tex_object, tc);	\n" + 
				"}"
			};System.out.println(fsrc[0]);
		
		gl.glShaderSource(fraShader, 1, fsrc, null);
		gl.glCompileShader(fraShader);
		printShaderInfoLog(gl, fraShader);
		
		shaderProgram = gl.glCreateProgram();
		gl.glAttachShader(shaderProgram, verShader);
		gl.glAttachShader(shaderProgram, fraShader);
		gl.glLinkProgram(shaderProgram);
		gl.glValidateProgram(shaderProgram);
		printProgramInfoLog(gl, shaderProgram);
		
		gl.glDeleteShader(verShader);
		gl.glDeleteShader(fraShader);
		
		offsetLoc = 0;
		verPosLoc = 1;
		tc_inLoc = 2;
	}
	
	public void setupBuffers(GL4 gl)
	{
		float[] vert = {
				0.25f, 0.25f, 0.75f, 1.0f, 
		        0.25f, -0.25f, 0.75f, 1.0f, 
		        -0.25f, -0.25f, 0.75f, 1.0f, 
		        -0.25f, 0.25f, 0.75f, 1.0f
		};
		
		vertBuff.put(vert);
		//vertBuff.put(texCoords);
		vertBuff.rewind();
		
		//notice that the generation and binding of vao must
		//be performed before setting up buffers otherwise you
		//will not be able to invoke the methods like "glEnableVertexAttribArray"
		//and "glVertexAttribPointer"
		gl.glGenVertexArrays(1, vaoNameBuff);
		gl.glBindVertexArray(vaoNameBuff.get(0));
		
		gl.glGenBuffers(1, vboNameBuff);
		gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vboNameBuff.get(0));
		//notice that buffer stores data in bits, thus the size of float (32bits, 
		//that is 4 bytes per float value) should be taken into consideration
		gl.glBufferData(GL4.GL_ARRAY_BUFFER, 16 * Float.SIZE / 8, vertBuff, GL4.GL_STATIC_DRAW);
		
		//gl.glVertexAttribPointer(tc_inLoc, 2, GL4.GL_FLOAT, false, 0, 16 * 4);
	}
	
	public void setupTextures(GL4 gl)
	{
		// Load texture from image
	    try {
	    	// Create a OpenGL Texture object from (URL, mipmap, file suffix)
	    	// Use URL so that can read from JAR and disk file.
	    	image = ImageIO.read(new File("images/tree.png"));
	    	tex = AWTTextureIO.newTexture(GLProfile.getDefault(), image, false);
	        
	    	//Method 2:
	    	//int[] textures = new int[1];
	    	//FileInputStream stream = new FileInputStream("images/tree.png");
	        //TextureData data = TextureIO.newTextureData(gl.getGLProfile(),stream, false, "png");
	        //tex = TextureIO.newTexture(data);
	    	
	        // Use linear filter for texture if image is larger than the original texture
	        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR);
	        // Use linear filter for texture if image is smaller than the original texture
	        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR);
	        
	        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE); 
	        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE); 
	    }
	    catch (GLException e)
	    {
	    	e.printStackTrace();
	    }
	    catch (IOException e)
	    {
	    	e.printStackTrace();
	    }
	}

	public void printShaderInfoLog(GL4 gl, int shader)
	{
		IntBuffer intBuffer = IntBuffer.allocate(1);
		gl.glGetShaderiv(shader, GL4.GL_INFO_LOG_LENGTH, intBuffer);
		
		if (intBuffer.get(0) > 12)
		{
			int size = intBuffer.get(0);
			
			System.err.println("Shader compiling error: ");
			ByteBuffer byteBuffer = ByteBuffer.allocate(size);
			gl.glGetShaderInfoLog(shader, size, intBuffer, byteBuffer);
			
			for (byte b : byteBuffer.array())
			{
				System.err.print((char) b);
			}
			
			System.exit(1);
		}
	}
	
	public void printProgramInfoLog(GL4 gl, int program)
	{
		IntBuffer intBuffer = IntBuffer.allocate(1);
		gl.glGetProgramiv(program, GL4.GL_INFO_LOG_LENGTH, intBuffer);
		
		if (intBuffer.get(0) > 1)
		{
			int size = intBuffer.get(0);
			
			System.err.println("Shader compiling error: ");
			ByteBuffer byteBuffer = ByteBuffer.allocate(size);
			gl.glGetProgramInfoLog(program, size, intBuffer, byteBuffer);
			
			for (byte b : byteBuffer.array())
			{
				System.err.print((char) b);
			}
			
			System.exit(1);
		}
	}
	
	public void init(GLAutoDrawable drawable)
	{
		GL4 gl = drawable.getGL().getGL4();
		GLSLMovingTexturedRect.test2(gl);
		//gl.glEnable(GL4.GL_DEPTH_TEST);
		//gl.glDepthFunc(GL4.GL_LEQUAL);
		
		initShaders(gl);
		setupBuffers(gl);
		setupTextures(gl);
	}
	
	public void dispose(GLAutoDrawable drawable)
	{
		GL4 gl = drawable.getGL().getGL4();
		
		gl.glDeleteVertexArrays(1, vaoNameBuff);
		gl.glDeleteProgram(shaderProgram);
	}
	
	public void display(GLAutoDrawable drawable)
	{
		GL4 gl = drawable.getGL().getGL4();
		
		float attrib[] = {
				(float) Math.sin(temp) * 0.5f,
				(float) Math.cos(temp) * 0.6f,
				0.0f, 0.0f
				};
		
		if (temp < 3.14f)
		{
			temp += 0.01f;
		}
		else
		{
			temp = 0f;
		}
		
		gl.glClearBufferfv(GL4.GL_COLOR, 0, green, 0);
		gl.glUseProgram(shaderProgram);
		
		//gl.glActiveTexture(GL4.GL_TEXTURE0);
        tex.enable(gl);
        tex.bind(gl);
		
		//update the offset value
		gl.glVertexAttrib4fv(offsetLoc, attrib, 0);
		
		gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vboNameBuff.get(0));
		gl.glEnableVertexAttribArray(verPosLoc);
		//gl.glEnableVertexAttribArray(tc_inLoc);
		//notice that the offset value is also calculated in bits, thus the size of 
		//float (32bits, that is 4 bytes per float value) should be taken into consideration
		gl.glVertexAttribPointer(verPosLoc, 4, GL4.GL_FLOAT, false, 0, 0);
		
		gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, tbo.get(0));
		gl.glEnableVertexAttribArray(tc_inLoc);
		//gl.glEnableVertexAttribArray(tc_inLoc);
		//notice that the offset value is also calculated in bits, thus the size of 
		//float (32bits, that is 4 bytes per float value) should be taken into consideration
		gl.glVertexAttribPointer(tc_inLoc, 2, GL4.GL_FLOAT, false, 0, 0);
		
		gl.glDrawArrays(GL4.GL_QUADS, 0, 4);
	}
	
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
	{
		GL4 gl = drawable.getGL().getGL4();
        
		if (height <= 0)
		{
            height = 1;
        }
        
        gl.glViewport(x, y, width, height);
	}
	
	public static void test2(GL4 gl)
	{
		texBuff = GLBuffers.newDirectFloatBuffer(8);
		
		float texCoords[] = {
				1.0f, 1.0f, 
		        1.0f, 0.0f, 
		        0.0f, 0.0f, 
		        0.0f, 1.0f
		};
		
		texBuff.put(texCoords);
		texBuff.rewind();
		
		gl.glGenBuffers(1, tbo);
		gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, tbo.get(0));
		gl.glBufferData(GL4.GL_ARRAY_BUFFER, 8 * Float.SIZE / 8, texBuff, GL4.GL_STATIC_DRAW);
		
		try {
	    	// Create a OpenGL Texture object from (URL, mipmap, file suffix)
	    	// Use URL so that can read from JAR and disk file.
	    	image2 = ImageIO.read(new File("images/tree.png"));
	    	tex2 = AWTTextureIO.newTexture(GLProfile.getDefault(), image2, false);
	    }
	    catch (IOException e)
	    {
	    	e.printStackTrace();
	    }
	}
	
	/** The entry main() method to setup the top-level container and animator */
	public static void main(String[] args)
	{
		// Run the GUI codes in the event-dispatching thread for thread safety
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				// Create the OpenGL rendering canvas
	            GLCanvas canvas = new GLSLMovingTexturedRect();
	            canvas.setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));

	            // Create a animator that drives canvas' display() at the specified FPS. 
	            final FPSAnimator animator = new FPSAnimator(canvas, FPS, true);
	            
	            // Create the top-level container
	            final JFrame frame = new JFrame(); // Swing's JFrame or AWT's Frame
	            frame.getContentPane().add(canvas);
	            frame.addWindowListener(new WindowAdapter()
	            {
	            	public void windowClosing(WindowEvent e)
	            	{
	            		// Use a dedicate thread to run the stop() to ensure that the
	            		// animator stops before program exits.
	            		new Thread()
	            		{
	            			public void run()
	            			{
	            				if (animator.isStarted())
	            				{
	            					animator.stop();
	            				}
	            				System.exit(0);
	            			}
	            		}.start();
	            	}
	            });
	            frame.setTitle(TITLE);
	            frame.pack();
	            frame.setVisible(true);
	            animator.start(); // start the animation loop
			}
		});
	}
}