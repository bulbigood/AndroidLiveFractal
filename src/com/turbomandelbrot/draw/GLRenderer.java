package com.turbomandelbrot.draw;

import java.nio.*;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;

import com.turbomandelbrot.logic.Explorer;
import com.turbomandelbrot.logic.MathFunctions;

import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_HIGH_FLOAT;

public class GLRenderer implements Renderer {

    // Our matrices
    private final float[] mtrxProjection = new float[16];
    private final float[] mtrxView = new float[16];
    private final float[] mtrxProjectionAndView = new float[16];

    // Geometric variables
    public static short indices[];
    public static float vertices[];
    public static float uvs[];
    public static float ymirror_uvs[];
    public static float xmirror_uvs[];

    public ShortBuffer drawListBuffer;
    public FloatBuffer vertexBuffer;
    public FloatBuffer uvBuffer;
    public FloatBuffer ymirror_uvBuffer;
    public FloatBuffer xmirror_uvBuffer;

    public static int srcFramebufferID[];
    public static int srcTextureID[];
    public static int lastDynamicBuffer = 1;

    public static int mirrorCopyFramebufferID[];
    public static int mirrorCopyTextureID[];
    public static int mirrorTextureIndex = 3;

    public static int colorGradientTextureID[];
    public static int colorGradientTextureIndex = 6;

    public static int textTextureID[];
    public static int textTextureIndex = 7;

    public TextManager tm;

    private float[] screenDimens = {640, 480};
    private float[] screenQ = {640, 480};
    private float mirrorScreenThreshold = 1.0f / 10.0f;


    private float[] edge_detection_step = {0, 0};

    /**
     * Выделение границ и заливка однородных областей "старыми" пикселями. Состояния 0 и 1
     */
    static int edge_detection_mode = 1;

    private Context mContext;
    private Explorer explorer;
    private static ShaderLoader shaderLoader;

    private static boolean evenRender = false;
    private static int renderTimes = 0;
    private final static int neededRendersToAllBuffers = 3;

    static int color_mode = 1;
    static int colorsNum;
    static float[] fractal_color = {255, 255, 255, 1};
    //static float[] fractal_color = {0, 0, 0, 1};
    static float[] back_color = {0, 0, 0, 1};
    static float[] colorsArray;

    public GLRenderer(Context c, Explorer ex) {
        mContext = c;
        explorer = ex;
        shaderLoader = new ShaderLoader(c.getAssets());
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //passGLrequirements();

        int[] rangeArray = new int[2];
        int[] precisionArray = new int[2];
        IntBuffer rangeBuf, precisionBuf;

        ByteBuffer bb = ByteBuffer.allocateDirect(2 * 4);
        bb.order(ByteOrder.nativeOrder());
        rangeBuf = bb.asIntBuffer();
        rangeBuf.put(rangeArray);
        rangeBuf.position(0);

        bb = ByteBuffer.allocateDirect(2 * 4);
        bb.order(ByteOrder.nativeOrder());
        precisionBuf = bb.asIntBuffer();
        precisionBuf.put(precisionArray);
        precisionBuf.position(0);

        GLES20.glGetShaderPrecisionFormat(GL_FRAGMENT_SHADER, GL_HIGH_FLOAT, rangeBuf, precisionBuf);
        int precision = precisionBuf.get();
        int rangeA = rangeBuf.get(0);
        int rangeB = rangeBuf.get(1);

        String extensions = GLES20.glGetString(GL10.GL_EXTENSIONS);
        boolean npot_tex = extensions.contains("GL_OES_texture_npot");

        // Set the clear color to black
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        colorsNum = 6;
        colorsArray = new float[colorsNum * 4];
        colorsArray[0] = 255; colorsArray[1] = 0; colorsArray[2] = 0; colorsArray[3] = 255;
        colorsArray[4] = 255; colorsArray[5] = 106; colorsArray[6] = 0; colorsArray[7] = 255;
        colorsArray[8] = 255; colorsArray[9] = 216; colorsArray[10] = 0; colorsArray[11] = 255;
        colorsArray[12] = 0; colorsArray[13] = 255; colorsArray[14] = 0; colorsArray[15] = 255;
        colorsArray[16] = 0; colorsArray[17] = 148; colorsArray[18] = 255; colorsArray[19] = 255;
        colorsArray[20] = 178; colorsArray[21] = 0; colorsArray[22] = 255; colorsArray[23] = 255;

        // Create the shaders, images
        int vertexShader = shaderLoader.loadShader(GLES20.GL_VERTEX_SHADER, shaderLoader.vs_Image);

        int fragmentShader = shaderLoader.loadShader(GL_FRAGMENT_SHADER, shaderLoader.fs_RegionMandelbrot);
        shaderLoader.sp_RegionFractal = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderLoader.sp_RegionFractal, vertexShader);
        GLES20.glAttachShader(shaderLoader.sp_RegionFractal, fragmentShader);
        GLES20.glLinkProgram(shaderLoader.sp_RegionFractal);

        fragmentShader = shaderLoader.loadShader(GL_FRAGMENT_SHADER, shaderLoader.fs_Image);
        shaderLoader.sp_Image = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderLoader.sp_Image, vertexShader);
        GLES20.glAttachShader(shaderLoader.sp_Image, fragmentShader);
        GLES20.glLinkProgram(shaderLoader.sp_Image);

        // Text shader
        int vshadert = shaderLoader.loadShader(GLES20.GL_VERTEX_SHADER, shaderLoader.vs_Text);
        int fshadert = shaderLoader.loadShader(GL_FRAGMENT_SHADER, shaderLoader.fs_Text);
        shaderLoader.sp_Text = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderLoader.sp_Text, vshadert);
        GLES20.glAttachShader(shaderLoader.sp_Text, fshadert);
        GLES20.glLinkProgram(shaderLoader.sp_Text);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        renderTimes = 0;

        screenDimens[0] = width;
        screenDimens[1] = height;

        screenQ[0] = width * explorer.getQuality();
        screenQ[1] = height * explorer.getQuality();

        edge_detection_step[0] = 1 / screenQ[0];
        edge_detection_step[1] = 1 / screenQ[1];

        // Redo the Viewport, making it fullscreen.
        GLES20.glViewport(0, 0, (int) screenDimens[0], (int) screenDimens[1]);

        // Clear our matrices
        for (int i = 0; i < 16; i++) {
            mtrxProjection[i] = 0.0f;
            mtrxView[i] = 0.0f;
            mtrxProjectionAndView[i] = 0.0f;
        }

        // Setup our screen width and height for normal sprite translation.
        Matrix.orthoM(mtrxProjection, 0, 0f, screenDimens[0], 0.0f, screenDimens[1], 0, 50);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mtrxView, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxProjection, 0, mtrxView, 0);

        // Create the triangles
        SetupTriangle();

        //Render-to-Texture
        srcFramebufferID = new int[2];
        srcTextureID = new int[2];
        createFramebufferTextures((int) screenQ[0], (int) screenQ[1], srcFramebufferID, srcTextureID, 2);

        mirrorCopyFramebufferID = new int[1];
        mirrorCopyTextureID = new int[1];
        createFramebufferTextures((int) screenQ[0], (int) screenQ[1], mirrorCopyFramebufferID, mirrorCopyTextureID, 1);

        // Create the image information
        SetupImage();
        // Create our texts
        SetupText("60 FPS");
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        explorer.calculatePosition();

        // Render fractal
        Render(mtrxProjectionAndView);

        // Render the text
        if(explorer.showingFPS()) {
            SetupText(String.valueOf(Math.round(explorer.getFPS())) + " FPS");
            if (tm != null)
                tm.Draw(mtrxProjectionAndView);
        }

        if(renderTimes < neededRendersToAllBuffers)
            renderTimes++;
    }

    private void Render(float[] m) {
        if(!explorer.isStatic() || renderTimes < neededRendersToAllBuffers) {
            int srcFB = evenRender? 0:1;
            int dstFB = evenRender? 1:0;
            lastDynamicBuffer = dstFB;
            evenRender = !evenRender;

            float[] lbCorner = MathFunctions.getLBCornerPos(explorer.posValues(), screenQ);
            float[] prev_pos = MathFunctions.getLBCornerPos(explorer.prevPosValues(), screenQ);
            if(renderTimes < neededRendersToAllBuffers)
                prev_pos = Explorer.NULL_POS;

            //Устанавливаем предыдущую текстуру для шейдера
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + srcFB);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, srcTextureID[srcFB]);

            //Инициализируем переменные для оптимизации симметрии
            int symmetry = 0;
            float[] texPos = {0,0};
            float[] offset_coord = {0, 0};

            //Определяем координаты симметрии
            saveSymmetryData(texPos, offset_coord);
            if(texPos[0] != 0 || texPos[1] != 0)
                symmetry = 1;

            GLES20.glViewport(0, 0, (int) screenQ[0], (int) screenQ[1]);

            if (lbCorner[2] == prev_pos[2]) {
                offset_coord[0] = Math.round((lbCorner[0] - prev_pos[0]) * lbCorner[2]);
                offset_coord[1] = Math.round((lbCorner[1] - prev_pos[1]) * lbCorner[2]);

                setFramebuffer(srcFramebufferID[dstFB]);
                drawFractal(m, lbCorner, prev_pos, offset_coord, srcFB);

                GLES20.glViewport((int) -offset_coord[0], (int) -offset_coord[1], (int) screenQ[0], (int) screenQ[1]);
                drawImage(m, uvBuffer, srcFB);
            } else if (symmetry == 1) {
                float[] bounded_pos = new float[3];
                bounded_pos[0] = texPos[0] / lbCorner[2] + lbCorner[0];
                bounded_pos[1] = texPos[1] / lbCorner[2] + lbCorner[1];
                bounded_pos[2] = lbCorner[2];

                setFramebuffer(mirrorCopyFramebufferID[0]);
                //GLES20.glViewport(0, 0, (int) screenQ[0], (int) screenQ[1]);
                drawFractal(m, bounded_pos, prev_pos, offset_coord, srcFB);

                //Если действует оптимизация симметрии, то рисуем результирующее изображение
                setFramebuffer(srcFramebufferID[dstFB]);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + mirrorTextureIndex);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mirrorCopyTextureID[0]);

                GLES20.glViewport((int) texPos[0], (int) texPos[1], (int) screenQ[0], (int) screenQ[1]);
                drawImage(m, uvBuffer, mirrorTextureIndex);

                GLES20.glViewport((int) offset_coord[0], (int) offset_coord[1], (int) screenQ[0], (int) screenQ[1]);
                drawImage(m, ymirror_uvBuffer, mirrorTextureIndex);

                //GLES20.glViewport(0, 0, (int) screenDimens[0], (int) screenDimens[1]);
            } else {
                offset_coord[0] = -screenQ[0];
                offset_coord[1] = -screenQ[1];

                setFramebuffer(srcFramebufferID[dstFB]);
                drawFractal(m, lbCorner, prev_pos, offset_coord, srcFB);
            }
        }

        setFramebuffer(0);
        GLES20.glViewport(0, 0, (int) screenDimens[0], (int) screenDimens[1]);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + lastDynamicBuffer);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, srcTextureID[lastDynamicBuffer]);

        drawImage(m, uvBuffer, lastDynamicBuffer);

        GLES20.glActiveTexture(0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    private void drawFractal(float[] m, float[] lb_position, float[] lb_prev_position, float[] offset_coord, int texID) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + colorGradientTextureIndex);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, colorGradientTextureID[0]);

        // Set our shaderprogram to image shader
        int shaderProgram = shaderLoader.sp_RegionFractal;
        GLES20.glUseProgram(shaderProgram);

        // get handle to vertex shader's vPosition member and add vertices
        int mPositionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        // Get handle to texture coordinates location and load the texture uvs
        int mTexCoordLoc = GLES20.glGetAttribLocation(shaderProgram, "a_texCoord");
        GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);
        GLES20.glEnableVertexAttribArray(mTexCoordLoc);
        // Get handle to shape's transformation matrix and add our matrix
        int mtrxhandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, m, 0);


        int mTexSizeHandle = GLES20.glGetUniformLocation(shaderProgram, "texSize");
        GLES20.glUniform2fv(mTexSizeHandle, 1, screenQ, 0);
        int mOffsetCoordHandle = GLES20.glGetUniformLocation(shaderProgram, "offset_coord");
        GLES20.glUniform2fv(mOffsetCoordHandle, 1, offset_coord, 0);


        int mPosHandle = GLES20.glGetUniformLocation(shaderProgram, "pos");
        GLES20.glUniform3fv(mPosHandle, 1, lb_position, 0);
        int mPrevCoordHandle = GLES20.glGetUniformLocation(shaderProgram, "prev_pos");
        GLES20.glUniform3fv(mPrevCoordHandle, 1, lb_prev_position, 0);


        int mSampler1Loc = GLES20.glGetUniformLocation(shaderProgram, "prev_frame");
        GLES20.glUniform1i(mSampler1Loc, texID);


        int mIterNumHandle = GLES20.glGetUniformLocation(shaderProgram, "iterationsNum");
        GLES20.glUniform1i(mIterNumHandle, explorer.getIterationsNum());


        int mColorsHandle = GLES20.glGetUniformLocation(shaderProgram, "fractal_color");
        GLES20.glUniform4fv(mColorsHandle, 1, fractal_color, 0);
        mColorsHandle = GLES20.glGetUniformLocation(shaderProgram, "back_color");
        GLES20.glUniform4fv(mColorsHandle, 1, back_color, 0);


        mColorsHandle = GLES20.glGetUniformLocation(shaderProgram, "colors");
        GLES20.glUniform4fv(mColorsHandle, colorsNum, colorsArray, 0);
        int mColorsNumHandle = GLES20.glGetUniformLocation(shaderProgram, "alternation_colors_num");
        GLES20.glUniform1f(mColorsNumHandle, (float)colorsNum);


        int mGradientLoc = GLES20.glGetUniformLocation(shaderProgram, "color_gradient");
        GLES20.glUniform1i(mGradientLoc, colorGradientTextureIndex);


        int mXaosStepHandle = GLES20.glGetUniformLocation(shaderProgram, "antiflickering_new_pixel_weight");
        GLES20.glUniform1f(mXaosStepHandle, explorer.getAntiflickeringPixelWeight());
        int mEdgeOffsetHandle = GLES20.glGetUniformLocation(shaderProgram, "edge_detection_step");
        GLES20.glUniform2fv(mEdgeOffsetHandle, 1, edge_detection_step, 0);


        int mColorModeLoc = GLES20.glGetUniformLocation(shaderProgram, "color_mode");
        GLES20.glUniform1i(mColorModeLoc, color_mode);
        mColorModeLoc = GLES20.glGetUniformLocation(shaderProgram, "antiflickering_mode");
        GLES20.glUniform1i(mColorModeLoc, explorer.antiflickeringEnabled());
        mColorModeLoc = GLES20.glGetUniformLocation(shaderProgram, "edge_detection_mode");
        GLES20.glUniform1i(mColorModeLoc, edge_detection_mode);

        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordLoc);
        GLES20.glUseProgram(0);
    }

    void drawImage(float[] matrix, FloatBuffer uvBuffer, int texIndex) {
        int shaderProgram = shaderLoader.sp_Image;
        GLES20.glUseProgram(shaderProgram);

        int mPositionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        int mTexCoordLoc = GLES20.glGetAttribLocation(shaderProgram, "a_texCoord");
        GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);
        GLES20.glEnableVertexAttribArray(mTexCoordLoc);
        int mtrxhandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, matrix, 0);
        int mSamplerLoc = GLES20.glGetUniformLocation(shaderProgram, "s_texture");
        GLES20.glUniform1i(mSamplerLoc, texIndex);

        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordLoc);
        GLES20.glUseProgram(0);
    }

    private void setFramebuffer(int ID) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, ID);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    }

    private void createFramebufferTextures(int width, int height, int[] framebufferID, int[] textureID, int number){
        GLES20.glGenFramebuffers(number, framebufferID, 0);
        GLES20.glGenTextures(number, textureID, 0);

        for(int i = 0; i < number; i++) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebufferID[i]);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, framebufferID[i]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, width, height, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textureID[i], 0);
        }
    }

    private void saveSymmetryData(float[] texPos, float[] offset_coord){
        float[] offset = {screenQ[0] * mirrorScreenThreshold, screenQ[1] * mirrorScreenThreshold};
        float[] symmetryCoord = {0, 0};
        if(explorer.getFractal().X_SYMMETRY) {
            symmetryCoord[0] = Math.round((explorer.getFractal().POINT_OF_SYMMETRY[0] -
                    MathFunctions.getLBCornerPos(explorer.posValues(), screenQ)[0]) * explorer.scale());

            if(symmetryCoord[0] > offset[0] && symmetryCoord[0] < screenQ[0] - offset[0]){
                //Определяем бОльшую часть экрана, которую нужно отрисовать и затем зеркалить
                if(symmetryCoord[0] > screenQ[0]/2){
                    texPos[0] = symmetryCoord[0] - screenQ[0];
                    offset_coord[0] = symmetryCoord[0];
                } else {
                    texPos[0] = symmetryCoord[0];
                    offset_coord[0] = symmetryCoord[0] - screenQ[0];
                }
            }
        }
        if(explorer.getFractal().Y_SYMMETRY) {
            symmetryCoord[1] = Math.round((explorer.getFractal().POINT_OF_SYMMETRY[1] -
                    MathFunctions.getLBCornerPos(explorer.posValues(), screenQ)[1]) * explorer.scale());

            if(symmetryCoord[1] > offset[1] && symmetryCoord[1] < screenQ[1] - offset[1]){
                //Определяем бОльшую часть экрана, которую нужно отрисовать и затем зеркалить
                if(symmetryCoord[1] > screenQ[1]/2){
                    texPos[1] = symmetryCoord[1] - screenQ[1];
                    offset_coord[1] = symmetryCoord[1];
                } else {
                    texPos[1] = symmetryCoord[1];
                    offset_coord[1] = symmetryCoord[1] - screenQ[1];
                }
            }
        }
    }

    public void SetupImage() {
        uvs = new float[]{
                0, 0,
                0, 1,
                1, 1,
                1, 0,
        };
        xmirror_uvs = new float[]{
                1, 0,
                1, 1,
                0, 1,
                0, 0,
        };
        ymirror_uvs = new float[]{
                0, 1,
                0, 0,
                1, 0,
                1, 1,
        };

        // The texture buffer
        ByteBuffer bb = ByteBuffer.allocateDirect(uvs.length * 4);
        bb.order(ByteOrder.nativeOrder());
        uvBuffer = bb.asFloatBuffer();
        uvBuffer.put(uvs);
        uvBuffer.position(0);

        bb = ByteBuffer.allocateDirect(ymirror_uvs.length * 4);
        bb.order(ByteOrder.nativeOrder());
        ymirror_uvBuffer = bb.asFloatBuffer();
        ymirror_uvBuffer.put(ymirror_uvs);
        ymirror_uvBuffer.position(0);

        bb = ByteBuffer.allocateDirect(xmirror_uvs.length * 4);
        bb.order(ByteOrder.nativeOrder());
        xmirror_uvBuffer = bb.asFloatBuffer();
        xmirror_uvBuffer.put(xmirror_uvs);
        xmirror_uvBuffer.position(0);

        textTextureID = new int[1];
        GLES20.glGenTextures(1, textTextureID, 0);

        // Again for the text texture
        int id = mContext.getResources().getIdentifier("drawable/font", null, mContext.getPackageName());
        Bitmap bmp = BitmapFactory.decodeResource(mContext.getResources(), id);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textTextureIndex);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textTextureID[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        bmp.recycle();

        colorGradientTextureID = new int[1];
        GLES20.glGenTextures(1, colorGradientTextureID, 0);
        id = mContext.getResources().getIdentifier("drawable/fire_gradient2", null, mContext.getPackageName());
        bmp = BitmapFactory.decodeResource(mContext.getResources(), id);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + colorGradientTextureIndex);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, colorGradientTextureID[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        bmp.recycle();
    }

    public void SetupTriangle() {
        // Our collection of vertices
        vertices = new float[]{
                0, 0, 0f,
                0, screenDimens[1], 0f,
                screenDimens[0], screenDimens[1], 0f,
                screenDimens[0], 0, 0f
        };

        // The indices for all textured quads
        indices = new short[]{0, 1, 2, 0, 2, 3};

        // The vertex buffer.
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(indices);
        drawListBuffer.position(0);
    }

    public void SetupText(String text) {
        // Create our text manager
        tm = new TextManager(shaderLoader);

        // Tell our text manager to use index 1 of textures loaded
        tm.setTextureID(textTextureIndex);

        // Pass the uniform scale
        tm.setUniformscale(Math.max(screenDimens[0], screenDimens[1]) / 500);

        // Create our new textobject
        TextObject txt = new TextObject(text, 1f, 1f);

        // Add it to our manager
        tm.addText(txt);

        // Prepare the text for rendering
        tm.PrepareDraw();
    }

    public void onPause() {
        renderTimes = 0;
        explorer.stopTimer();
    }

    public static boolean passGLrequirements(){
        int[] rangeArray = new int[2];
        int[] precisionArray = new int[2];
        IntBuffer rangeBuf, precisionBuf;

        ByteBuffer bb = ByteBuffer.allocateDirect(2 * 4);
        bb.order(ByteOrder.nativeOrder());
        rangeBuf = bb.asIntBuffer();
        rangeBuf.put(rangeArray);
        rangeBuf.position(0);

        bb = ByteBuffer.allocateDirect(2 * 4);
        bb.order(ByteOrder.nativeOrder());
        precisionBuf = bb.asIntBuffer();
        precisionBuf.put(precisionArray);
        precisionBuf.position(0);

        GLES20.glGetShaderPrecisionFormat(GL_FRAGMENT_SHADER, GL_HIGH_FLOAT, rangeBuf, precisionBuf);
        int precision = precisionBuf.get();
        int rangeA = rangeBuf.get(0);
        int rangeB = rangeBuf.get(1);

        String extensions = GLES20.glGetString(GL10.GL_EXTENSIONS);
        boolean npot_tex = true;
        if(extensions != null){
            npot_tex = extensions.contains("GL_OES_texture_npot");
        }

        return true;
    }
}
