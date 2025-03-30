package com.rpk16.bbqcontroller

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {

    private val _controllerIp = MutableLiveData<String?>()
    val controllerIp: LiveData<String?> = _controllerIp

    fun setControllerIp(ip: String) {
        _controllerIp.postValue(ip)
    }
}