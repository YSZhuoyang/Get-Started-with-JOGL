import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
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
 * @version 3.02
 */
public class GLSLUseBuffer extends GLCanvas implements GLEventListener 
{
	private static final long serialVersionUID = 1L;
	
	// Define constants for the top-level container
	private static String TITLE = "Use buffers";
	private static final int CANVAS_WIDTH = 320;  // width of the drawable
	private static final int CANVAS_HEIGHT = 240; // height of the drawable
	private static final int FPS = 60; // animator's target frames per second
	
	private IntBuffer vaoNameBuff = GLBuffers.newDirectIntBuffer(1);
	private IntBuffer vboNameBuff = GLBuffers.newDirectIntBuffer(1);
	private FloatBuffer vertBuff = GLBuffers.newDirectFloatBuffer(12);
	
	private int shaderProgram;
	
	private int verPosLoc;
	private int offsetLoc;
	
	private float green[] = { 0.0f, 0.25f, 0.0f, 1.0f };
	
	private float temp;
	
	public GLSLUseBuffer()
	{
		float[] vert = {
				0.25f, -0.25f, 0f, 1.0f, 
				-0.25f, -0.25f, 0f, 1.0f, 
				0.25f, 0.25f, 0f, 1.0f
		};
		vertBuff.put(vert);
		vertBuff.rewind();
		
		addGLEventListener(this);
	}
	
	public void init(GLAutoDrawable drawable)
	{
		GL4 gl = drawable.getGL().getGL4();
		
		initShaders(gl);
		setupBuffers(gl);
	}
	
	public void initShaders(GL4 gl)
	{
		int verShader = gl.glCreateShader(GL4.GL_VERTEX_SHADER);
		int fraShader = gl.glCreateShader(GL4.GL_FRAGMENT_SHADER);
		
		String[] vsrc = 
			{
				"#version 400							\n" + 
				"layout (location = 0) in vec4 offset;	\n" + 
				"layout (location = 1) in vec4 position;\n" + 
				"void main(void)						\n" + 
				"{										\n" + 
				"	gl_Position = position + offset;	\n" + 
				"}"
			};System.out.println(vsrc[0]);
		
		gl.glShaderSource(verShader, 1, vsrc, null);
		gl.glCompileShader(verShader);
		printShaderInfoLog(gl, verShader);
		
		String[] fsrc = 
			{
				"#version 400			\n" + 
				// Output to the framebuffer
				"out vec4 color;		\n" + 
				"void main(void)		\n" + 
				"{						\n" + 
				"	color = vec4(0.0, 0.8, 1.0, 1.0);	\n" + 
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
		//printProgramInfoLog(gl, shaderProgram);
		
		IntBuffer intBuffer = IntBuffer.allocate(1);
	      gl.glGetProgramiv(shaderProgram, GL4.GL_LINK_STATUS, intBuffer);
	      
	      if (intBuffer.get(0) != 1)
	      {
	          gl.glGetProgramiv(shaderProgram, GL4.GL_INFO_LOG_LENGTH, intBuffer);
	          int size = intBuffer.get(0);
	          System.err.println("Program link error: ");
	          if (size > 0)
	          {
	              ByteBuffer byteBuffer = ByteBuffer.allocate(size);
	              gl.glGetProgramInfoLog(shaderProgram, size, intBuffer, byteBuffer);
	              for (byte b : byteBuffer.array())
	              {
	                  System.err.print((char) b);
	              }
	          }
	          else
	          {
	              System.out.println("Unknown");
	          }
	          System.exit(1);
	      }
		
		gl.glDeleteShader(verShader);
		gl.glDeleteShader(fraShader);
		
		verPosLoc = gl.glGetAttribLocation(shaderProgram, "position");
		offsetLoc = gl.glGetAttribLocation(shaderProgram, "offset");
	}
	
	public void setupBuffers(GL4 gl)
	{
		gl.glGenVertexArrays(1, vaoNameBuff);
		gl.glBindVertexArray(vaoNameBuff.get(0));
		
		gl.glGenBuffers(1, vboNameBuff);
		gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vboNameBuff.get(0));
		//notice that buffer stores data in bits, thus the size of float (32bits, 
		//that is 4 bytes per float value) should be taken into consideration
		gl.glBufferData(GL4.GL_ARRAY_BUFFER, 12 * Float.SIZE / 8, vertBuff, GL4.GL_STATIC_DRAW);
		gl.glEnableVertexAttribArray(verPosLoc);
		//notice that the offset value is also calculated in bits, thus the size of 
		//float (32bits, that is 4 bytes per float value) should be taken into consideration
		gl.glVertexAttribPointer(verPosLoc, 4, GL4.GL_FLOAT, false, 0, 0);
	}
	
	public String printShaderInfoLog(GL4 gl, int shader)
	{
		final int logLen = getShaderParameter(gl, shader, GL4.GL_INFO_LOG_LENGTH);
		
		if (logLen <= 0)
		{
			return "";
		}
		
		final int[] retLength = new int[1];
		final byte[] bytes = new byte[logLen + 1];
		gl.glGetShaderInfoLog(shader, logLen, retLength, 0, bytes, 0);
		final String logMessage = new String(bytes);
		
		return String.format("ShaderLog: %s", logMessage);
	}
	
	public String printProgramInfoLog(GL4 gl, int program)
	{
		final int logLen = getProgramParameter(gl, program, GL4.GL_INFO_LOG_LENGTH);
		
		if (logLen <= 0)
		{
			return "";
		}
		
		final int[] retLength = new int[1];
		final byte[] bytes = new byte[logLen + 1];
		gl.glGetProgramInfoLog(program, logLen, retLength, 0, bytes, 0);
		final String logMessage = new String(bytes);
		
		return String.format("ShaderLog: %s", logMessage);
	}
	
	private int getShaderParameter(GL4 gl, int shader, int paramName)
	{
		final int params[] = new int[1];
		
		gl.glGetShaderiv(shader, paramName, params, 0);
		
		return params[0];
	}
	
	private int getProgramParameter(GL4 gl, int program, int paramName)
	{
		final int params[] = new int[1];
		
		gl.glGetProgramiv(program, paramName, params, 0);
		
		return params[0];
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
		
		//update the offset value
		gl.glVertexAttrib4fv(offsetLoc, attrib, 0);
		
		gl.glDrawArrays(GL4.GL_TRIANGLES, 0, 3);
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
	
	/** The entry main() method to setup the top-level container and animator */
	public static void main(String[] args)
	{
		// Run the GUI codes in the event-dispatching thread for thread safety
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				// Create the OpenGL rendering canvas
	            GLCanvas canvas = new GLSLUseBuffer();
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