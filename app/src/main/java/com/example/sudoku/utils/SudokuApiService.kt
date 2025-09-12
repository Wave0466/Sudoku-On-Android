package com.example.sudoku.utils

import com.example.sudoku.model.SudokuApiResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// 定义 API 接口
interface SudokuApiService {
    @GET("generate") // 对应接口地址的最后一部分
    suspend fun generateSudoku(
        @Query("key") apiKey: String,
        @Query("difficulty") difficulty: String
    ): SudokuApiResponse
}

// 创建一个全局唯一的 Retrofit 实例
object RetrofitInstance {
    private const val BASE_URL = "http://apis.juhe.cn/fapig/sudoku/"

    val api: SudokuApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SudokuApiService::class.java)
    }
}