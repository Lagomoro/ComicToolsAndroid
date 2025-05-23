package com.moroworks.comictools.ui.epaper

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GoodDisplayNFCePaperViewModel : ViewModel() {

    private val _textWaitNFCLaunch = MutableLiveData<String>().apply {
        value = "等待 NFC 启动"
    }
    val textWaitNFCLaunch: LiveData<String> = _textWaitNFCLaunch

    private val _textBtnNfcStart = MutableLiveData<String>().apply {
        value = "开始写入 NFC 智能价签"
    }
    val textBtnNfcStart: LiveData<String> = _textBtnNfcStart

}