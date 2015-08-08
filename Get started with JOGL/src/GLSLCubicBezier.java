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
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;


/**
 * @author Yu Sangzhuoyang
 * @version 3.10
 */
public class GLSLCubicBezier extends GLCanvas implements GLEventListener
{
	private static final long serialVersionUID = 1L;
	
	private static String TITLE = "Test glsl CubBezierTess";
	private static final int CANVAS_WIDTH = 320;  // width of the drawable
	private static final int CANVAS_HEIGHT = 240; // height of the drawable
	private static final int FPS = 60; // animator's target frames per second
	
	private IntBuffer vao;
	private IntBuffer vbo;
	
	private int shaderProgram;
	
	private float black[] = { 0.1f, 0.1f, 0.0f, 1.0f };
	
	private PMVMatrix pmvMat;
	
	private int posLoc;
	private int mvMatLoc;
	private int proMatLoc;
	
	private float[] mvMat;
	private float[] proMat;
	
	public GLSLCubicBezier()
	{
		mvMat = new float[16];
		proMat = new float[16];
		
		posLoc = 0;
		mvMatLoc = 3;
		proMatLoc = 4;
		
		pmvMat = new PMVMatrix();
		
		vao = GLBuffers.newDirectIntBuffer(1);
		vbo = GLBuffers.newDirectIntBuffer(1);
		
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
				//"layout (location = 4) uniform mat4 proMatrix;	\n" + 
				"layout (location = 0) in vec4 position;		\n" + 
				"void main(void)								\n" + 
				"{												\n" + 
				"	gl_Position = mvMatrix * position;			\n" + 
                "}												\n"
			};
		
		gl.glShaderSource(vs, 1, vs_source, null);
		gl.glCompileShader(vs);
		printShaderInfoLog(gl, vs);
		
		String[] tcs_source = 
			{
				"#version 430 core							\n" + 
				"layout (vertices = 16) out;				\n" + 
				"void main(void)							\n" + 
				"{											\n" + 
				"	if (gl_InvocationID == 0)				\n" + 
				"	{										\n" + 
				"		gl_TessLevelInner[0] = 16.0;		\n" + 
				"		gl_TessLevelInner[1] = 16.0;		\n" + 
				"		gl_TessLevelOuter[0] = 16.0;		\n" + 
				"		gl_TessLevelOuter[1] = 16.0;		\n" + 
				"		gl_TessLevelOuter[2] = 16.0;		\n" + 
				"		gl_TessLevelOuter[3] = 16.0;		\n" + 
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
				"layout (quads, equal_spacing, cw) in;			\n" + 
				//"layout (location = 3) uniform mat4 mvMatrix;	\n" + 
				"layout (location = 4) uniform mat4 proMatrix;	\n" + 
				"out vec3 normal;								\n" + 
				"												\n" + 
				"vec4 quad_bezier(vec4 A, vec4 B, vec4 C, float t)	\n" + 
				"{												\n" + 
				"	vec4 D = mix(A, B, t);						\n" + 
				"	vec4 E = mix(B, C, t);						\n" + 
				"												\n" + 
				"	return mix(D, E, t);						\n" + 
				"}												\n" + 
				"												\n" + 
				"vec4 cubic_bezier(vec4 A, vec4 B, vec4 C, vec4 D, float t)	\n" + 
				"{												\n" + 
				"	vec4 E = mix(A, B, t);						\n" + 
				"	vec4 F = mix(B, C, t);						\n" + 
				"	vec4 G = mix(C, D, t);						\n" + 
				"												\n" + 
				"	return quad_bezier(E, F, G, t);				\n" + 
				"}												\n" + 
				"												\n" + 
				"vec4 evaluate_patch(vec2 at)					\n" + 
				"{												\n" + 
				"	vec4 P[4];									\n" + 
				"	int i;										\n" + 
				"												\n" + 
				"	for (i = 0; i < 4; i++)						\n" + 
				"	{											\n" + 
				"		P[i] = cubic_bezier(gl_in[i + 0].gl_Position, 	\n" + 
				"							gl_in[i + 4].gl_Position, 	\n" + 
				"							gl_in[i + 8].gl_Position, 	\n" + 
				"							gl_in[i + 12].gl_Position, \n" + 
				"							at.y);				\n" + 
				"	}											\n" + 
				"												\n" + 
				"	return cubic_bezier(P[0], P[1], P[2], P[3], at.x);	\n" + 
				"}												\n" + 
				"												\n" + 
				//used to create another two points forming three to 
				//calculate the normal vector
				"float epsilon = 0.001;							\n" + 
				"												\n" + 
				"void main(void)								\n" + 
				"{												\n" + 
				"	vec4 p1 = evaluate_patch(gl_TessCoord.xy);	\n" + 
				"	vec4 p2 = evaluate_patch(gl_TessCoord.xy + vec2(0.0, epsilon));	\n" + 
				"	vec4 p3 = evaluate_patch(gl_TessCoord.xy + vec2(epsilon, 0.0));	\n" + 
				"												\n" + 
				"	vec3 v1 = normalize(p2.xyz - p1.xyz);		\n" + 
				"	vec3 v2 = normalize(p3.xyz - p2.xyz);		\n" + 
				"												\n" + 
				"	normal = normalize(cross(v1, v2));			\n" + 
				"												\n" + 
				"	gl_Position = proMatrix * p1;				\n" + 
                "}"
			};
		
		gl.glShaderSource(tesEvaShader, 1, tes_source, null);
		gl.glCompileShader(tesEvaShader);
		printShaderInfoLog(gl, tesEvaShader);
		
		String[] fs_source = 
			{
				"#version 430 core							\n" + 
				"out vec4 color;							\n" + 
				"in vec3 normal;							\n" + 
				"void main(void)							\n" + 
				"{											\n" + 
				"	vec4 c = vec4(1.0, -1.0, 0, 0) * normal.z + " + 
				"				vec4(0, 0, 0, 1.0);			\n" + 
				"	color = clamp(c, vec4(0.0), vec4(1.0));	\n" + 
                "}"
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
	}
	
	public void setupVertices(GL4 gl)
	{
		float[] patch_coords = {
				-1.0f, 0.3f, 1.0f, 1.0f, 
		        -0.33f, -2.0f, 1.0f, 1.0f, 
		         0.33f, -1.0f, 1.0f, 1.0f, 
		         1.0f, 1.0f,  1.0f, 1.0f, 
		         
		        -1.0f, -0.33f, 0.3f, 1.0f, 
		        -0.33f, 0.0f, 0.3f, 1.0f, 
		         0.33f, -0.0f, 0.3f, 1.0f, 
		         1.0f, -0.1f, 0.3f, 1.0f, 
		         
		        -1.0f, 0.33f, -0.3f, 1.0f, 
		        -0.33f, 0.33f, -0.3f, 1.0f, 
		         0.33f, 0.33f, -0.3f, 1.0f, 
		         1.0f, 0.33f, -0.3f, 1.0f, 
		         
		        -1.0f, 1.0f, -1.0f, 1.0f, 
		        -0.33f, 0.0f, -1.0f, 1.0f, 
		         0.33f, -0.3f, -1.0f, 1.0f, 
		         1.0f, 1.0f, -1.0f, 1.0f
		};
		
		int vertBuffSize = patch_coords.length;
		FloatBuffer vertBuff = GLBuffers.newDirectFloatBuffer(vertBuffSize);
		
		vertBuff.put(patch_coords);
		vertBuff.rewind();
		
		gl.glGenVertexArrays(1, vao);
		gl.glBindVertexArray(vao.get(0));
		
		gl.glGenBuffers(1, vbo);
		gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vbo.get(0));
		gl.glBufferData(GL4.GL_ARRAY_BUFFER, vertBuffSize * Float.SIZE / 8, vertBuff, GL4.GL_STATIC_DRAW);
		
		gl.glVertexAttribPointer(posLoc, 4, GL4.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(posLoc);
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
		setupVertices(gl);
		
		//gl.glEnable(GL4.GL_DEPTH_TEST);
		gl.glPatchParameteri(GL4.GL_PATCH_VERTICES, 16);
		//gl.glBindVertexArray(vao.get(0));
		//gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_LINE);
	}
	
	public void dispose(GLAutoDrawable drawable)
	{
		GL4 gl = drawable.getGL().getGL4();
		
		gl.glDeleteVertexArrays(1, vao);
		gl.glDeleteProgram(shaderProgram);
	}
	
	public void display(GLAutoDrawable drawable)
	{
		GL4 gl = drawable.getGL().getGL4();
		//gl.glClearBufferfv(GL4.GL_DEPTH, 0, new float[1], 0);
		gl.glClearBufferfv(GL4.GL_COLOR, 0, black, 0);
		setCamera(0f, 0f, 5f, 0f, 0f, -1f, 0f, 1f, 0f);
		gl.glUseProgram(shaderProgram);
		setUniforms(gl);
		
		//gl.glBindVertexArray(vao.get(0));
		gl.glDrawArrays(GL4.GL_PATCHES, 0, 16);
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
  				GLCanvas canvas = new GLSLCubicBezier();
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