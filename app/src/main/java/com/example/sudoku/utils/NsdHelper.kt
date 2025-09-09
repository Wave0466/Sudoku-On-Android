package com.example.sudoku.utils

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

class NsdHelper(private val context: Context) {

    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var resolveListener: NsdManager.ResolveListener? = null

    private val SERVICE_TYPE = "_sudoku._tcp."
    private val TAG = "NsdHelper"

    interface NsdListener {
        fun onServiceDiscovered(service: NsdServiceInfo)
        fun onServiceLost(service: NsdServiceInfo)
        fun onServiceRegistered(serviceInfo: NsdServiceInfo)
        fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int)
        fun onServiceResolved(serviceInfo: NsdServiceInfo)
        fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int)
    }

    private var listener: NsdListener? = null

    fun setNsdListener(listener: NsdListener) {
        this.listener = listener
    }

    fun registerService(port: Int, serviceName: String) {
        unregisterService()
        initializeRegistrationListener()
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            this.serviceType = SERVICE_TYPE
            this.port = port
        }
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private fun initializeRegistrationListener() {
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "服务注册成功: ${serviceInfo.serviceName}")
                listener?.onServiceRegistered(serviceInfo)
            }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "服务注册失败: Error code: $errorCode")
                listener?.onRegistrationFailed(serviceInfo, errorCode)
            }
            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Log.d(TAG, "服务已注销: ${arg0.serviceName}")
            }
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "服务注销失败: Error code: $errorCode")
            }
        }
    }

    fun discoverServices() {
        stopDiscovery()
        initializeDiscoveryListener()
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private fun initializeDiscoveryListener() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "服务发现成功: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceType != SERVICE_TYPE) {
                    return
                }

                // 恢复了关键逻辑: 发现服务后，立刻开始解析它
                resolveService(serviceInfo)

                // 同时也回调 onServiceDiscovered，让UI可以即时更新文本
                listener?.onServiceDiscovered(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.e(TAG, "服务丢失: ${serviceInfo.serviceName}")
                listener?.onServiceLost(serviceInfo)
            }

            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "服务发现已启动")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(TAG, "服务发现已停止: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "启动服务发现失败: Error code: $errorCode")
                try { nsdManager.stopServiceDiscovery(this) } catch (e: Exception) {}
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "停止服务发现失败: Error code: $errorCode")
                try { nsdManager.stopServiceDiscovery(this) } catch (e: Exception) {}
            }
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        initializeResolveListener()
        nsdManager.resolveService(serviceInfo, resolveListener)
    }

    private fun initializeResolveListener() {
        resolveListener = object : NsdManager.ResolveListener {
            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "服务解析成功: IP=${serviceInfo.host}, Port=${serviceInfo.port}")
                listener?.onServiceResolved(serviceInfo)
            }
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "服务解析失败: ${serviceInfo.serviceName}, Error code: $errorCode")
                listener?.onResolveFailed(serviceInfo, errorCode)
            }
        }
    }

    fun stopDiscovery() {
        if (discoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (e: Exception) {
                Log.e(TAG, "停止发现时出错", e)
            } finally {
                discoveryListener = null
            }
        }
    }

    fun unregisterService() {
        if (registrationListener != null) {
            try {
                nsdManager.unregisterService(registrationListener)
            } catch (e: Exception) {
                Log.e(TAG, "注销服务时出错", e)
            } finally {
                registrationListener = null
            }
        }
    }

    fun tearDown() {
        unregisterService()
        stopDiscovery()
    }
}