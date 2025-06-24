package com.example.file_module

import android.content.Context
import android.util.Log


class FileSystem {

    companion object {
        // Used to load the 'file_module' library on application startup.
        init {
            System.loadLibrary("file_module")
        }
    }

    private val businessId = "user_profiles"


    // 初始化文件管理器
    external fun initManager(basePath: String?, cacheSize: Int, useAsync: Boolean): Boolean

    // 文件操作
    external fun createFile(businessId: String?, filename: String?, content: String?): Boolean
    external fun readFile(businessId: String?, filename: String?): String?
    external fun updateFile(businessId: String?, filename: String?, content: String?): Boolean
    external fun appendFile(businessId: String?, filename: String?, content: String?): Boolean
    external fun deleteFile(businessId: String?, filename: String?): Boolean
    external fun fileExists(businessId: String?, filename: String?): Boolean

    /**
     * 文件目录操作
     * @param substr 指定字符； 传空为获取所有文件
     * @param day 获取指定天数的文件, 传0为获取所有文件, 传1为获取当天的文件, 传2为获取两天的文件, 以此类推
     * @param flag true: 时间正序(传0为获取所有文件, 传1为获取当天的文件, 传2为获取两天的文件, 以此类推)
     *             false: 时间倒序 (传0为获取所有文件，传1为获取一天之前的所有文件，传2为获取两天之前的所有文件，以此类推)
     */
    external fun prefetchDirectory(businessId: String?, substr: String = "", day: Int = 0, flag: Boolean = true) : List<String>?

    // 应用中使用示例
    fun init(context: Context) {
        val dir = context.filesDir
        initManager(dir.absolutePath, 1500, true)
    }

    fun saveUserProfile(userId: String?, json: String?): Boolean {
        val isExists = fileExists(businessId, "$userId.json")
        return if (isExists) {
            updateFile(businessId, "$userId.json", json)
        } else {
            createFile(businessId, "$userId.json", json)
        }
    }

    fun loadUserProfile(userId: String?): String? {
        return readFile(businessId, "$userId.json")
    }

    fun loadPrefetchDirectory(substr: String = "", day: Int = 0, flag: Boolean = true) : List<String>? {
        return prefetchDirectory(businessId, substr = substr, day = day, flag = flag)
    }

    fun appendFile(userId: String?, json: String?): Boolean {
        return appendFile(businessId, "$userId.json", json)
    }

    fun deleteUserProfile(userId: String?): Boolean {
        return deleteFile(businessId, "$userId.json")
    }
}