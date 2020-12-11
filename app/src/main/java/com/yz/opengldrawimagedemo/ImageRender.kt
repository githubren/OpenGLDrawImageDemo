package com.yz.opengldrawimagedemo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 *
 * @author RenBing
 * @date 2020/12/7 0007
 */
class ImageRender(val context: Context) : GLSurfaceView.Renderer{

    //顶点着色器
    private val vertexMatrixShaderCode = """attribute vec4 vPosition;
attribute vec2 vCoordinate;
uniform mat4 vMatrix;
varying vec2 aCoordinate;
void main(){
    gl_Position=vMatrix*vPosition;
    aCoordinate=vCoordinate;
}"""

    //片元着色器
    private val fragmentShaderCode = """precision mediump float;
uniform sampler2D vTexture;
varying vec2 aCoordinate;
void main(){
    gl_FragColor=texture2D(vTexture,aCoordinate);
}"""

    /*
        1、顶点坐标系坐标原点在屏幕中央，分x、y、z三个坐标轴 正方向分别为右、上、垂直屏幕向外；
        2、纹理坐标系坐标原点在屏幕左上角，x、y正方向分别为右、下；
        3、OpenGL中只有点、线、三角形这三种图元，所以在设置坐标点的时候要以能组成三角形的三个点来设置；
        4、顶点坐标和纹理坐标顺序能够决定图形的旋转角度。
     */

    //顶点坐标
    private val sPos = floatArrayOf(
        -1f,1f,
        -1f,-1f,
        1f,1f,
        1f,-1f
    )
    //纹理坐标
    private val sCoord = floatArrayOf(
        0f,0f,
        0f,1f,
        1f,0f,
        1f,1f
    )

    private var bitmap: Bitmap? = null
    private var program = 0
    private var glHPosition = 0
    private var glHTexture = 0
    private var glHCoordinate = 0
    private var glHMatrix = 0
    private val viewMatrix = FloatArray(16)
    private val projectMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var bPos: FloatBuffer? = null
    private var bCoord: FloatBuffer? = null

    init {
        //获取需要显示的图片
        bitmap = ContextCompat.getDrawable(context,R.drawable.img)?.toBitmap()
        //为存放形状的坐标，初始化顶点字节缓冲,float占4个字节
        val bb = ByteBuffer.allocateDirect(sPos.size*4)
        //使用设备的本点字节序
        bb.order(ByteOrder.nativeOrder())
        //为ByteBuffer创建一个浮点缓冲
        bPos = bb.asFloatBuffer()
        //把顶点坐标加入FloatBuffer
        bPos?.put(sPos)
        //设置buffer，从第一个坐标开始读
        bPos?.position(0)
        val cc = ByteBuffer.allocateDirect(sCoord.size*4)
        cc.order(ByteOrder.nativeOrder())
        bCoord = cc.asFloatBuffer()
        bCoord?.put(sCoord)
        bCoord?.position(0)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(glHMatrix,1,false,mvpMatrix,0)
        GLES20.glEnableVertexAttribArray(glHPosition)
        GLES20.glEnableVertexAttribArray(glHCoordinate)
        GLES20.glUniform1i(glHTexture,0)

        val texture = IntArray(1)
        if (bitmap?.isRecycled == false){
            //生成纹理
            GLES20.glGenTextures(1,texture,0)
            //绑定纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture[0])
            //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
            //设置放大过滤为使用纹理中坐标最近的若干个颜色，通过加权平均算法绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
            //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]，将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat())
            //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]，将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat())
            //根据以上参数生成一个2d纹理
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bitmap,0)
            //传入顶点坐标
            GLES20.glVertexAttribPointer(glHPosition,2,GLES20.GL_FLOAT,false,0,bPos)
            //传入纹理坐标
            GLES20.glVertexAttribPointer(glHCoordinate,2,GLES20.GL_FLOAT,false,0,bCoord)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4)

        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0,0,width, height)
        val w = bitmap?.width?:0
        val h = bitmap?.height?:0
        val sWH = w.toFloat()/h
        val sWidthHeight = width.toFloat()/height
        if (width>height){
            if (sWH>sWidthHeight){
                Matrix.orthoM(projectMatrix,0, (-sWidthHeight*sWH).toFloat(),
                    (sWidthHeight*sWH).toFloat(),-1f,1f,3f,7f
                )
            }else{
                Matrix.orthoM(projectMatrix,0, (-sWidthHeight/sWH).toFloat(),
                    (sWidthHeight/sWH).toFloat(),-1f,1f,3f,7f
                )
            }
        }else{
            if (sWH>sWidthHeight){
                Matrix.orthoM(projectMatrix,0,-1f,1f, (-1/sWidthHeight*sWH).toFloat(),
                    (1/sWidthHeight*sWH).toFloat(),3f,7f)
            }else{
                Matrix.orthoM(projectMatrix,0,-1f,1f,
                    (-sWH/sWidthHeight).toFloat(), (sWH/sWidthHeight).toFloat(),3f,7f)
            }
        }
        //设置相机位置
        Matrix.setLookAtM(viewMatrix,0,0f,0f,7f,0f,0f,0f,0f,1f,0f)
        //计算变换矩阵
        Matrix.multiplyMM(mvpMatrix,0,projectMatrix,0,viewMatrix,0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(1f,1f,1f,1f)
        GLES20.glEnable(GLES20.GL_TEXTURE_2D)
        val vertextShader = loadShader(GLES20.GL_VERTEX_SHADER,vertexMatrixShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER,fragmentShaderCode)
        program = GLES20.glCreateProgram()
        if (program != 0){
            GLES20.glAttachShader(program,vertextShader)
            GLES20.glAttachShader(program,fragmentShader)
            GLES20.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,linkStatus,0)
            if (linkStatus[0] != GLES20.GL_TRUE){
                GLES20.glDeleteProgram(program)
                program = 0
            }
        }
        // 获取指向vertex shader(顶点着色器)的成员vPosition的handle
        //glGetAttribLocation方法：获取着色器程序中，指定为attribute类型变量的id。
        //glGetUniformLocation方法：获取着色器程序中，指定为uniform类型变量的id。
        glHPosition = GLES20.glGetAttribLocation(program,"vPosition")
        glHCoordinate = GLES20.glGetAttribLocation(program,"vCoordinate")
        glHTexture = GLES20.glGetUniformLocation(program,"vTexture")
        glHMatrix = GLES20.glGetUniformLocation(program,"vMatrix")
    }

    /**
     * 加载着色器
     * @return
     */
    private fun loadShader(type:Int,shaderCode:String):Int {
        //创建顶点着色器
        val vertextShader = GLES20.glCreateShader(type)
        //将着色器代码加到着色器中
        GLES20.glShaderSource(vertextShader, shaderCode)
        //编译着色器
        GLES20.glCompileShader(vertextShader)
        if (vertextShader == 0){
            Log.e("TAG","loaded shader failed")
        }
        return vertextShader
    }
}