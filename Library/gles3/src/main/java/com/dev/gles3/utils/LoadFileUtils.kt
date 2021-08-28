package com.dev.gles3.utils

import android.content.Context
import android.content.res.Resources
import java.io.*


/**
 * description : 文件加载
 *
 * @author     : hudongxin
 * @date       : 8/14/21
 */
object LoadFileUtils {

    /**
     * 读取资源
     * @param resourceId
     */
    fun readResource(context: Context, resourceId: Int): String {
        val builder = StringBuilder()
        try {
            val inputStream: InputStream = context.resources.openRawResource(resourceId)
            val streamReader = InputStreamReader(inputStream)
            val bufferedReader = BufferedReader(streamReader)
            var textLine: String?
            while (bufferedReader.readLine().also { textLine = it } != null) {
                builder.append(textLine)
                builder.append("\n")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Resources.NotFoundException) {
            e.printStackTrace()
        }
        return builder.toString()
    }

    /**
     * 从Asset目录读取资源
     */
    fun loadResourceFromAsset(context: Context, sourceName: String): String {
        val shaderSource = StringBuffer()
        try {
            val br = BufferedReader(InputStreamReader(context.assets.open(sourceName)))
            var tempStr: String?
            while (null != br.readLine().also { tempStr = it }) {
                shaderSource.append(tempStr)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return shaderSource.toString()
    }

    /**
     * 将Asset目录下文件copy到sdcard
     */
    fun copyFileIfNeed(context: Context, sourceName: String): Int {
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            // 默认存储在data/data/<包名>/file目录下
            val modelFile = File(context.filesDir, sourceName)
            inputStream = context.assets.open(sourceName)
            if (modelFile.length().toInt() == inputStream.available()) {
                return -1
            }
            outputStream = FileOutputStream(modelFile)
            val buffer = ByteArray(1024)
            var length = inputStream.read(buffer)
            while (length > 0) {
                outputStream.write(buffer, 0, length)
                length = inputStream.read(buffer)
            }
            outputStream.flush()
        } catch (e: IOException) {
            e.printStackTrace()
            return -1
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return 0
    }
}