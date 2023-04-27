package com.example.myjsontest01

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import com.google.gson.GsonBuilder
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

// JSON 変換テスト用クラス
data class MyJsonElement (
    val name: String,
    val department: String,
    val year: Int,
    val car: Boolean,
    val date: Date,
)

// JSON 変換テスト用クラス
data class MyJsonElementRoot (
    val rootkey: MyJsonElement
)

// JSON による POST テスト用のクラス.
data class MyJsonBeaconNotify (
    val inside_region: Boolean,
    val date: Date,
    val message: String,
)

// 状態の定義
enum class MyStatusEnum {
    STAT_IDLE,
    STAT_STARTED,
    STAT_SUCCESSED,
    STAT_FAILED,
}

class MainActivity : AppCompatActivity() {

    companion object {
        val TAG = "Otou"

        val JSONSTRING_FOR_MYJSONELEMENT = """
            |{"name":"佐藤","department":"開発部","year":2001,"car":true,"date":"2022-07-08 21:31:15"}
        """.trimMargin()

        val JSONSTRING_FOR_MYJSONELEMENTROOT = """
            |{"rootkey":
            |  {"name":"佐藤","department":"開発部","year":2001,"car":true,"date":"2022-07-08 21:31:15"}
            |}
        """.trimMargin()

        val CONNECTION_TIMEOUT_MILLISECONDS = 10000
        val READ_TIMEOUT_MILLISECONDS = 10000
    }

    var stat = MyStatusEnum.STAT_IDLE

    // OkHttpClient を作成
    val client = OkHttpClient.Builder()
        .connectTimeout(CONNECTION_TIMEOUT_MILLISECONDS.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(READ_TIMEOUT_MILLISECONDS.toLong(), TimeUnit.MILLISECONDS)
        .build()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var gsan = GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        // --- JSON 文字列から Java オブジェクトへの変換テスト 1
        Log.i(TAG, "入力JSON文字列>$JSONSTRING_FOR_MYJSONELEMENT<")
        val element = gsan.fromJson(JSONSTRING_FOR_MYJSONELEMENT, MyJsonElement::class.java)
        Log.i(TAG, "変換した MyJsonElement オブジェクト:$element")
        // --- JSON 文字列から Java オブジェクトへの変換テスト 2
        val element2 = gsan.fromJson(JSONSTRING_FOR_MYJSONELEMENTROOT, MyJsonElementRoot::class.java)
        Log.i(TAG, "変換した MyJsonElementRoot オブジェクト:$element2")
        // --- Java オブジェクトから JSON 文字列への変換テスト
        var msg = gsan.toJson(element2)
        Log.i(TAG, "変換した JSON 文字列>$msg<")
        // ---- Java オブジェクトから JSON 文字列への変換テスト 2
        var x = MyJsonBeaconNotify(true, Date(System.currentTimeMillis()), "Hello")
        var msg2 = gsan.toJson(x)
        Log.i(TAG, "変換した JSON 文字列>$msg2<")
        // -----

        findViewById<Button>(R.id.mybutton_get).setOnClickListener {
            when(stat) {
                MyStatusEnum.STAT_IDLE, MyStatusEnum.STAT_SUCCESSED, MyStatusEnum.STAT_FAILED -> {
                    findViewById<TextView>(R.id.mytextView).text = "GET STARTED"
                    stat = MyStatusEnum.STAT_STARTED
                    Log.i(TAG, "<mybutton_get> start!")
                    sampleGetRequest()
                }
                MyStatusEnum.STAT_STARTED -> {
                    Log.i(TAG, "<mybutton_get> Already started!")
                }
                else -> {
                    Log.i(TAG, "<mybutton_get> Unknown")
                }
            }
        }

        findViewById<Button>(R.id.mybutton_post).setOnClickListener {
            when(stat) {
                MyStatusEnum.STAT_IDLE, MyStatusEnum.STAT_SUCCESSED, MyStatusEnum.STAT_FAILED -> {
                    findViewById<TextView>(R.id.mytextView).text = "POST STARTED"
                    stat = MyStatusEnum.STAT_STARTED
                    Log.i(TAG, "<mybutton_post> start!")
                    var is_inside_region = findViewById<Switch>(R.id.switch_inside).isChecked
                    var message = findViewById<EditText>(R.id.editTextMessage).text.toString()
                    samplePostRequest(is_inside_region, message)
                }
                MyStatusEnum.STAT_STARTED -> {
                    Log.i(TAG, "<mybutton_post> Already started!")
                }
                else -> {
                    Log.i(TAG, "<mybutton_post> Unknown")
                }
            }
        }
    }

    val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    fun samplePostRequest(is_inside_region: Boolean, message: String) {
        Log.i(TAG, "MainActivity#samplePostRequest() BEGIN")
        val instance = MyJsonBeaconNotify(is_inside_region, Date(System.currentTimeMillis()), message)

        var gsan = GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        gsan.toJson(instance)?.let {
            val request = Request.Builder().url("http://192.168.0.11/notify_beacon").post(it.toRequestBody(JSON_MEDIA)).build()

            client.newCall(request).enqueue(object : Callback {

                override fun onResponse(call: Call, response: Response) {
                    Log.i(TAG, "<samplePostRequest/onResponse> BEGIN")
                    Log.i(TAG, "<samplePostRequest/onResponse> code:${response.code}")
                    Log.i(TAG, "<samplePostRequest/onResponse> message:${response.message}")
                    val body = response.body?.string().orEmpty()
                    Log.i(TAG, "<samplePostRequest/onResponse> BODY:$body")
                    stat = MyStatusEnum.STAT_SUCCESSED
                    this@MainActivity.run {
                        runOnUiThread(Runnable {
                            findViewById<TextView>(R.id.mytextView).text = "SUCCEED"
                        })
                    }
                    Log.i(TAG, "<samplePostRequest/onResponse> DONE")
                }

                override fun onFailure(call: Call, e: IOException) {
                    Log.i(TAG, "<<samplePostRequest/onFailure>> onFailure() BEGIN")
                    stat = MyStatusEnum.STAT_FAILED
                    this@MainActivity.run {
                        runOnUiThread(Runnable {
                            findViewById<TextView>(R.id.mytextView).text = "FAILED"
                        })
                    }
                    Log.i(TAG, "<<samplePostRequest/onFailure>> onFailure() DONE")
                }
            })
        }
        Log.i(TAG, "MainActivity#samplePostRequest() DONE")
    }

    fun sampleGetRequest() {
        Log.i(TAG, "MainActivity#sampleGetRequest() BEGIN")

        var request = Request.Builder().url("http://192.168.0.11/create_token").build()
        client.newCall(request).enqueue(object : Callback {

            override fun onResponse(call: Call, response: Response) {
                Log.i(TAG, "<samplePostRequest/onResponse> BEGIN")
                Log.i(TAG, "<samplePostRequest/onResponse> code:${response.code}")
                Log.i(TAG, "<samplePostRequest/onResponse> message:${response.message}")
                stat = MyStatusEnum.STAT_SUCCESSED
                this@MainActivity.run {
                    runOnUiThread(Runnable {
                        findViewById<TextView>(R.id.mytextView).text = "SUCCEED"
                    })
                }
                Log.i(TAG, "<samplePostRequest/onResponse> DONE")
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.i(TAG, "<samplePostRequest/onFailure> BEGIN")
                Log.i(TAG, "<samplePostRequest/onFailure> ${e.toString()}")
                stat = MyStatusEnum.STAT_FAILED
                this@MainActivity.run {
                    runOnUiThread(Runnable {
                        findViewById<TextView>(R.id.mytextView).text = "FAILED"
                    })
                }
                Log.i(TAG, "<samplePostRequest/onFailure> DONE")
            }
        })
        Log.i(TAG, "MainActivity#sampleGetRequest() DONE")
    }
}