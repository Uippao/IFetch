package me.uippao.ifetch

import kotlinx.serialization.Serializable

@Serializable
data class NetworkInterfaceInfo(
    val name: String,
    val mac: String
)

@Serializable
data class IFetchInfo(
    val timestamp: String,
    val hostname: String,
    val osInfo: String,
    val externalIPv4: String,
    val externalIPv6: String,
    val lanIPs: List<String>,
    val networkInterfaces: List<NetworkInterfaceInfo>
)