package com.moroworks.comictools;

import android.app.PendingIntent
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.moroworks.comictools.databinding.ActivityEpaperBinding
import java.io.IOException

class GoodDisplayNFCePaperActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEpaperBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEpaperBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val textViewNfcResult: TextView = binding.textNfcResult
        textViewNfcResult.text = buildString {
            append("等待 NFC 启动")
        }

        val buttonNfcStart: Button = binding.buttonNfcStart
        buttonNfcStart.text = buildString {
            append("返回上一界面")
        }
        buttonNfcStart.setOnClickListener(NfcStartClickListener(this))
    }

    class NfcStartClickListener(private var activity: GoodDisplayNFCePaperActivity) : View.OnClickListener {

        override fun onClick(v: View) {
            activity.startActivity(Intent(activity, MainActivity::class.java))
        }

    }

    private var nfcAdapter: NfcAdapter? = null;

    override fun onResume() {
        super.onResume()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            binding.textNfcResult.text = buildString {
                append("该设备不支持 NFC，等待 NFC 启动")
            }
            return
        }
        if (!nfcAdapter!!.isEnabled) {
            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
        }

        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
        nfcAdapter!!.enableForegroundDispatch(this, pendingIntent, filters, null)

        binding.textNfcResult.text = buildString {
            append("NFC 准备就绪，请靠近 NFC 智能价签")
        }
    }

    override fun onPause() {
        super.onPause()

        nfcAdapter?.disableForegroundDispatch(this)
        binding.textNfcResult.text = buildString {
            append("NFC 后台待机，等待 NFC 启动")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if(tag != null){
                val thread: Thread = object : Thread() {
                    override fun run() {
                        writeTag(tag)
                    }
                }
                thread.start()
            }
        }
    }

    private fun writeTag(tag: Tag){
        binding.textNfcResult.text = buildString {
            append("数据解析中")
        }

        val dataText = getClipContent();
        val dataTextSplit = dataText.split('|');

        binding.textEpaper.text = buildString {
            append("数据长度：")
            append(dataText.length)
            append("  块长度：")
            append(dataTextSplit.size)
        };
        binding.textNfcResult.text = buildString {
            append("数据解析成功")
        }

        val tech = tag.techList
        if (tech[0] == IsoDep::class.java.name) {
            val isoDep = IsoDep.get(tag)
            try {
                isoDep.timeout = 50000
                if (!isoDep.isConnected) {
                    isoDep.connect()
                }
                if (isoDep.isConnected) {
                    binding.textNfcResult.text = buildString {
                        append("NFC 智能价签连接成功")
                    }

                    var response: ByteArray = byteArrayOf();
                    response = isoDep.transceive(hex2Bytes(dataTextSplit[0]));
                    response = isoDep.transceive(hex2Bytes(dataTextSplit[1]));
                    response = isoDep.transceive(hex2Bytes(dataTextSplit[2]));

                    if (response[0] == 0x90.toByte()) {
                        binding.textNfcResult.text = buildString {
                            append("NFC 智能价签初始化错误：90")
                        }
                    } else {
                        binding.textNfcResult.text = buildString {
                            append("NFC 智能价签初始化错误：未知错误")
                        }
                    }

                    binding.textNfcResult.text = buildString {
                        append("NFC 智能价签初始化成功")
                    }

                    try {
                        var index = 3;
                        while (index < dataTextSplit.size - 1) {
                            val dataBlock = dataTextSplit[index];
                            if(dataBlock.contains("=")){
                                val dataPair = dataBlock.split("=")
                                var hex = hex2Bytes(dataPair[0]);
                                var textIndex = 0;
                                while (textIndex < dataPair[1].length){
                                    val number = dataPair[1].substring(textIndex, textIndex + 4);
                                    hex += number.toUByte().toByte();
                                    textIndex += 4;
                                }
                                response = isoDep.transceive(hex);
                            }
                            index += 1;
                            binding.textNfcResult.text = buildString {
                                append("数据传输中，已传输：")
                                append(index)
                                append("，剩余：")
                                append(dataTextSplit.size - index - 1)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Error", e.toString())
                    }
                    binding.textNfcResult.text = buildString {
                        append("数据传输成功，请等待刷新结束")
                    }

                    response = isoDep.transceive(hex2Bytes(dataTextSplit[dataTextSplit.size - 1]));

                    if (response[0] == 0x90.toByte()) {
                        binding.textNfcResult.text = buildString {
                            append("NFC 智能价签开始刷新，请等待刷新结束")
                        }
                    } else {
                        binding.textNfcResult.text = buildString {
                            append("NFC 智能价签刷新错误：未知错误")
                        }
                    }
                }
            } catch (_: IOException) {

            } finally {
                if (nfcAdapter != null) {
                    try {
                        isoDep.close();
                    } catch (_: IOException) {

                    }
                }

            }
        }

    }

    private fun getClipContent(): String {
        val clipboardManager: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboardManager.hasPrimaryClip() && clipboardManager.primaryClip!!.itemCount > 0) {
            val text = clipboardManager.primaryClip!!.getItemAt(0).text.toString()
            if (!TextUtils.isEmpty(text)) {
                return text
            }
        }
        return ""
    }

    private fun parse(c: Char): Int {
        if (c >= 'a') return (c.code - 'a'.code + 10) and 0x0f
        if (c >= 'A') return (c.code - 'A'.code + 10) and 0x0f
        return (c.code - '0'.code) and 0x0f
    }

    private fun hex2Bytes(hex: String): ByteArray {
        val charArray = hex.toCharArray()
        val byteArray = ByteArray(hex.length / 2)
        var j = 0
        for (i in byteArray.indices) {
            val c0 = charArray[j++]
            val c1 = charArray[j++]
            byteArray[i] = ((parse(c0) shl 4) or parse(c1)).toByte();
        }
        return byteArray
    }

}
