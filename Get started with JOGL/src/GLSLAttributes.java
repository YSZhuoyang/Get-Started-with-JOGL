import java.awt.Dimension; 
import java.awt.event.WindowAdapter; 
import java.awt.event.WindowEvent; 
import java.nio.ByteBuffer;
import java.nio.IntBuffer; 

import javax.swing.JFrame; 
import javax.swing.SwingUtilities; 

import com.jogamp.opengl.util.FPSAnimator; 
import com.jogamp.opengl.util.GLBuffers; 
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;


/**
 * @author Yu Sangzhuoyang
 * @version 3.01
 */
public class GLSLAttributes extends GLCanvas implements GLEventListener 
{ 
	private static final long serialVersionUID = 1L; 
	
	// Define constants for the top-level container 
	private static String TITLE = "Test glsl"; 
	private static final int CANVAS_WIDTH = 320;  // width of the drawable 
	private static final int CANVAS_HEIGHT = 240; // height of the drawable 
	private static final int FPS = 60; // animator's target frames per second 
	
	private IntBuffer vaoNameBuff = GLBuffers.newDirectIntBuffer(1); 
	private int shaderProgram; 
	private float green[] = { 0.0f, 0.25f, 0.0f, 1.0f }; 
	
	private int offsetLoc;
	private float temp; 
	
	public GLSLAttributes() 
	{ 
		addGLEventListener(this); 
	} 
	
	public void init(GLAutoDrawable drawable) 
	{ 
		GL4 gl = drawable.getGL().getGL4(); 
		
		initShaders(gl); 
	} 
	
	public void initShaders(GL4 gl) 
	{
	   int verShader = gl.glCreateShader(GL4.GL_VERTEX_SHADER); 
	   int fraShader = gl.glCreateShader(GL4.GL_FRAGMENT_SHADER); 
	   
	   String[] vsrc = 
                    { 
                            "#version 430 core											\n" + 
                            "layout (location = 0) in vec4 offset;						\n" + 
                            "void main(void) 											\n" + 
                            "{        													\n" + 
                            " const vec4 vertices[] = vec4[](vec4(0.25, -0.25, 0.5, 1), \n" + 
                            " vec4(-0.25, -0.25, 0.5, 1),								\n" + 
                            " vec4( 0.25, 0.25, 0.5, 1));								\n" + 
                            " gl_Position = vertices[gl_VertexID] + offset;				\n" + 
                            "}" 
                    }; 

	   gl.glShaderSource(verShader, 1, vsrc, null); 
	   gl.glCompileShader(verShader); 
	   printShaderInfoLog(gl, verShader);
	   
	   String[] fsrc = 
                    { 
                            "#version 430 core							\n" + 
                            "out vec4 color;							\n" + 
                            "void main(void)							\n" + 
                            "{											\n" + 
                            " color = vec4(0.0, 0.8, 1.0, 1.0);	\n" + 
                            "}" 
                    }; 

	   gl.glShaderSource(fraShader, 1, fsrc, null); 
	   gl.glCompileShader(fraShader); 
	   printShaderInfoLog(gl, fraShader);
	   
	   shaderProgram = gl.glCreateProgram(); 
	   gl.glAttachShader(shaderProgram, verShader); 
	   gl.glAttachShader(shaderProgram, fraShader); 
	   gl.glLinkProgram(shaderProgram); 
	   gl.glValidateProgram(shaderProgram); 
	   printProgramInfoLog(gl, shaderProgram);
	   
	   gl.glGenVertexArrays(1, vaoNameBuff); 
	   gl.glBindVertexArray(vaoNameBuff.get(0)); 
	   
	   offsetLoc = gl.glGetAttribLocation(shaderProgram, "offset");
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
		
		if (temp < 6.28f)
		{
			temp += 0.01f;
		}
		else
		{
			temp = 0f;
		}
  		
  		gl.glClearBufferfv(GL4.GL_COLOR, 0, green, 0); 
  		gl.glUseProgram(shaderProgram); 
  		gl.glVertexAttrib4fv(offsetLoc, attrib, 0); 
  		gl.glDrawArrays(GL4.GL_TRIANGLES, 0, 3); 
  	} 
  	
  	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) 
  	{ 
  		
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
  				GLCanvas canvas = new GLSLAttributes(); 
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