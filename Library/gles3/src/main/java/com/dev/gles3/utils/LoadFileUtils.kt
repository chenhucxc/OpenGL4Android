package com.dev.gles3.utils

import android.content.Context
import android.content.res.Resources
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader


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
}