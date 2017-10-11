package com.turbomandelbrot.draw;

import android.content.res.AssetManager;
import android.opengl.GLES20;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Scanner;

public class ShaderLoader {

	// Program variables
	public int sp_Fractal;
	public int sp_MovedFractal;
	public int sp_RegionFractal;

	public int sp_Image;
	public int sp_Text;
	public int sp_SolidColor;

	private AssetManager assetManager;

	public ShaderLoader(AssetManager am){
		assetManager = am;

		vs_Image = readFile("shaders/Image.vsh");
		fs_Image = readFile("shaders/Image.fsh");
		fs_RegionMandelbrot = readFile("shaders/Mandelbrot_region.fsh");
	}
	
	/* SHADER Solid
	 * 
	 * This shader is for rendering a colored primitive.
	 * 
	 */
	public final String vs_SolidColor =
		"uniform 	mat4 		uMVPMatrix;" +
		"attribute 	vec4 		vPosition;" +
	    "void main() {" +
	    "  gl_Position = uMVPMatrix * vPosition;" +
	    "}";
	
	public final String fs_SolidColor =
		"precision mediump float;" +
	    "void main() {" +
	    "  gl_FragColor = vec4(0.5,0,0,1);" +
	    "}"; 
	
	/* SHADER Image
	 * 
	 * This shader is for rendering 2D images straight from a texture
	 * No additional effects.
	 * 
	 */
	public final String vs_Image;
	public final String fs_Image;

	/* SHADER Text
	 * 
	 * This shader is for rendering 2D text textures straight from a texture
	 * Color and alpha blended.
	 * 
	 */
	public final String vs_Text =
		"uniform mat4 uMVPMatrix;" +
		"attribute vec4 vPosition;" +
		"attribute vec4 a_Color;" +
		"attribute vec2 a_texCoord;" +
		"varying vec4 v_Color;" + 
		"varying vec2 v_texCoord;" +
	    "void main() {" +
	    "  gl_Position = uMVPMatrix * vPosition;" +
	    "  v_texCoord = a_texCoord;" +
	    "  v_Color = a_Color;" + 
	    "}";
	public final String fs_Text =
	    "precision mediump float;" +
	    "varying vec4 v_Color;" +
	    "varying vec2 v_texCoord;" +
        "uniform sampler2D s_texture;" +
	    "void main() {" +
	    "  gl_FragColor = texture2D( s_texture, v_texCoord ) * v_Color;" +
	    "  gl_FragColor.rgb *= v_Color.a;" +
	    "}";

	public final String fs_RegionMandelbrot;

	public int loadShader(int type, String shaderCode){

	    // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
	    // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
	    int shader = GLES20.glCreateShader(type);

	    // add the source code to the shader and compile it
	    GLES20.glShaderSource(shader, shaderCode);
	    GLES20.glCompileShader(shader);

		String err = getShaderErrorStackTrace(shader);
		if (err != null) {
			if(type == GLES20.GL_VERTEX_SHADER)
				Log.e("Vertex shader error", err);
			else
				Log.e("Fragment shader error", err);
		}

	    // return the shader
	    return shader;
	}

	public String getShaderErrorStackTrace(int shader){
		String trace = null;

		int[] compiled = new int[1];
		GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
		if (compiled[0] == 0) {
			trace = "Shader error! Could not compile program: ";

			//TODO wtf 9 * 4 ?
			ByteBuffer bb = ByteBuffer.allocateDirect(9 * 4);
			bb.order(ByteOrder.nativeOrder());
			IntBuffer intBuf = bb.asIntBuffer();
			intBuf.position(0);
			GLES20.glGetShaderiv(shader, GLES20.GL_INFO_LOG_LENGTH, intBuf);

			compiled[0]=intBuf.get(0);
			if (compiled[0]>1){
				trace += GLES20.glGetShaderInfoLog(shader);
			}
		}

		return trace;
	}

	public String readFile(String path) {
		String text = "";
		InputStream is = null;
		try {
			is = assetManager.open(path);
		} catch(IOException ioe){
			Log.e("File", ioe.getMessage());
		}
		Scanner sc = new Scanner(is);
		while(sc.hasNext()) {
			String line = sc.nextLine();
			String ws = line.replaceAll("\\s","");
			if(ws.length() < 2 || !ws.substring(0,2).equals("//"))
				text += line;
		}
		return text;
	}
}
