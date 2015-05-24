import java.awt.Dimension; 
import java.awt.event.WindowAdapter; 
import java.awt.event.WindowEvent; 
import java.nio.ByteBuffer;
import java.nio.IntBuffer; 
import javax.swing.JFrame; 
import javax.swing.SwingUtilities; 

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable; 
import com.jogamp.opengl.GLEventListener; 
import com.jogamp.opengl.awt.GLCanvas; 
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.PMVMatrix;


/**
 * @author Yu Sangzhuoyang
 * @version 3.03
 */
public class GLSLTessellation extends GLCanvas implements GLEventListener
{
	private static final long serialVersionUID = 1L;
	
	private static String TITLE = "Test glsl Tessellation";
	private static final int CANVAS_WIDTH = 320;  // width of the drawable
	private static final int CANVAS_HEIGHT = 240; // height of the drawable
	private static final int FPS = 60; // animator's target frames per second
	
	private IntBuffer vaoBuff = GLBuffers.newDirectIntBuffer(1);
	
	private int shaderProgram;
	
	private float green[] = { 0.0f, 0.25f, 0.0f, 1.0f };
	
	private PMVMatrix pmvMat;
	
	private int mvMatLoc;
	private int proMatLoc;
	
	private float[] mvMat;
	private float[] proMat;
	
	public GLSLTessellation()
	{
		mvMat = new float[16];
		proMat = new float[16];
		
		mvMatLoc = 3;
		proMatLoc = 4;
		
		pmvMat = new PMVMatrix();
		
		addGLEventListener(this);
	}
	
	public void initShaders(GL4 gl)
	{
		int vs = gl.glCreateShader(GL4.GL_VERTEX_SHADER);
		int tesConShader = gl.glCreateShader(GL4.GL_TESS_CONTROL_SHADER);
		int tesEvaShader = gl.glCreateShader(GL4.GL_TESS_EVALUATION_SHADER);
		int fs = gl.glCreateShader(GL4.GL_FRAGMENT_SHADER);
		
		String[] vs_source = 
			{
				"#version 430 core								\n" + 
				"layout (location = 3) uniform mat4 mvMatrix;	\n" + 
				"layout (location = 4) uniform mat4 proMatrix;	\n" + 
				"void main(void)								\n" + 
				"{												\n" + 
				"	const vec4 vertices[] = vec4[](vec4(0.25, -0.25, 0.5, 1.0),	\n" + 
				"	vec4(-0.25, -0.25, 0.5, 1.0),				\n" + 
                "	vec4( 0.25, 0.25, 0.5, 1.0));				\n" + 
				"	gl_Position = proMatrix * mvMatrix * vertices[gl_VertexID];	\n" + 
                "}												\n"
			};
		
		gl.glShaderSource(vs, 1, vs_source, null);
		gl.glCompileShader(vs);
		printShaderInfoLog(gl, vs);
		
		String[] tcs_source = 
			{
				"#version 430 core							\n" + 
				"layout (vertices = 3) out;					\n" + 
				"void main(void)							\n" + 
				"{											\n" + 
				"	if (gl_InvocationID == 0)				\n" + 
				"	{										\n" + 
				"		gl_TessLevelInner[0] = 5.0;			\n" + 
				"		gl_TessLevelOuter[0] = 5.0;			\n" + 
				"		gl_TessLevelOuter[1] = 5.0;			\n" + 
				"		gl_TessLevelOuter[2] = 5.0;			\n" + 
				"	}										\n" + 
				"	gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;\n" + 
                "}											\n"
			};
		
		gl.glShaderSource(tesConShader, 1, tcs_source, null);
		gl.glCompileShader(tesConShader);
		printShaderInfoLog(gl, tesConShader);
		
		String[] tes_source = 
			{
				"#version 430 core								\n" + 
				"layout (triangles, equal_spacing, cw) in;		\n" + 
				"void main(void)								\n" + 
				"{												\n" + 
				"	gl_Position = (gl_TessCoord.x * gl_in[0].gl_Position) + \n" + 
				"	(gl_TessCoord.y * gl_in[1].gl_Position) + 	\n" + 
				"	(gl_TessCoord.z * gl_in[2].gl_Position);	\n" + 
                "}												\n"
			};
		
		gl.glShaderSource(tesEvaShader, 1, tes_source, null);
		gl.glCompileShader(tesEvaShader);
		printShaderInfoLog(gl, tesEvaShader);
		
		String[] fs_source = 
			{
				"#version 430 core						\n" + 
				"out vec4 color;						\n" + 
				"void main(void)						\n" + 
				"{										\n" + 
				"	color = vec4(0.0, 0.8, 1.0, 1.0);	\n" + 
                "}										\n"
			};
		
		gl.glShaderSource(fs, 1, fs_source, null);
		gl.glCompileShader(fs);
		printShaderInfoLog(gl, fs);
		
		shaderProgram = gl.glCreateProgram();
		gl.glAttachShader(shaderProgram, vs);
		gl.glAttachShader(shaderProgram, tesConShader);
		gl.glAttachShader(shaderProgram, tesEvaShader);
		gl.glAttachShader(shaderProgram, fs);
		gl.glLinkProgram(shaderProgram);
		gl.glValidateProgram(shaderProgram);
		printProgramInfoLog(gl, shaderProgram);
		
		//mvMatLoc = gl.glGetUniformLocation(shaderProgram, "mvMatrix");
		//System.out.println("modelViewnMatrixLocation:" + mvMatLoc);
		//proMatLoc = gl.glGetUniformLocation(shaderProgram, "proMatrix");
		//System.out.println("projectionMatrixLocation:" + proMatLoc);
	}
	
	public void setCamera(float eyeX, float eyeY, float eyeZ, float centreX, float centreY, float centreZ, float upX, float upY, float upZ)
	{
		pmvMat.glLoadIdentity();
		pmvMat.gluLookAt(eyeX, eyeY, eyeZ, centreX, centreY, centreZ, upX, upY, upZ);
	}
	
	public void resetProAndMvMatrices(float aspect)
	{
		pmvMat.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		pmvMat.glLoadIdentity();
		pmvMat.gluPerspective(45.0f, aspect, 1.0f, 100f);
		
		pmvMat.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		pmvMat.glLoadIdentity();
	}
	
	public void setUniforms(GL4 gl)
	{
		pmvMat.glGetFloatv(GLMatrixFunc.GL_MODELVIEW, mvMat, 0);
		pmvMat.glGetFloatv(GLMatrixFunc.GL_PROJECTION, proMat, 0);
		
		gl.glUniformMatrix4fv(mvMatLoc, 1, false, mvMat, 0);
		gl.glUniformMatrix4fv(proMatLoc, 1, false, proMat, 0);
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
		
		initShaders(gl);
		
		gl.glGenVertexArrays(1, vaoBuff);
		gl.glBindVertexArray(vaoBuff.get(0));
		
		gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_LINE);
	}
	
	public void dispose(GLAutoDrawable drawable)
	{
		GL4 gl = drawable.getGL().getGL4();
		
		gl.glDeleteVertexArrays(1, vaoBuff);
		gl.glDeleteProgram(shaderProgram);
	}
	
	public void display(GLAutoDrawable drawable)
	{
		GL4 gl = drawable.getGL().getGL4();
		
		gl.glClearBufferfv(GL4.GL_COLOR, 0, green, 0);
		setCamera(0f, 0f, 3f, 0f, 0f, -1f, 0f, 1f, 0f);
		gl.glUseProgram(shaderProgram);
		setUniforms(gl);
		
		gl.glDrawArrays(GL4.GL_PATCHES, 0, 3);
	}
	
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
	{
		GL4 gl = drawable.getGL().getGL4();
		
		if (height == 0)
		{
			height = 1;
		}
		
		float aspect = (float) width / height;
		
		gl.glViewport(x, y, width, height);
		resetProAndMvMatrices(aspect);
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
  				GLCanvas canvas = new GLSLTessellation();
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