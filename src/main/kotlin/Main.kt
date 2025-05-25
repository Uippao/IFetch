package me.uippao.ifetch

import com.google.gson.GsonBuilder
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import kotlinx.serialization.Serializable
import com.charleskorn.kaml.Yaml
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.serialization.XML
import java.lang.System.getProperty
import java.net.*
import java.text.SimpleDateFormat
import java.util.*

fun getTimestamp(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())

fun getHostname(): String =
    try { InetAddress.getLocalHost().hostName } catch (e: Exception) { "Unknown" }

fun getOsInfo(): String = listOf(
    getProperty("os.name", "Unknown"),
    getProperty("os.version", "Unknown"),
    "${getProperty("os.arch", "Unknown")}"
).joinToString(" ") { if (it == getProperty("os.arch", "Unknown")) "($it)" else it }

fun getExternalIP(url: String, isIPv6: Boolean = false): String {
    return try {
        if (isIPv6 && !isIPv6Enabled()) return "Not configured"
        val (_, _, result) = url.httpGet().timeoutRead(2000).responseString()
        when (result) {
            is Result.Success -> {
                val ip = result.get().trim()
                if (ip.isNotBlank() && (!isIPv6 || ip.contains(":"))) ip else "Not configured"
            }
            else -> "Not configured"
        }
    } catch (e: Exception) {
        "Not configured"
    }
}

fun getPrimaryPrivateIPv4(): String? {
    val nics = NetworkInterface.getNetworkInterfaces().toList()
    for (nic in nics) {
        if (!nic.isUp || nic.isLoopback) continue
        for (addr in nic.inetAddresses.toList()) {
            if (addr is Inet4Address && addr.isSiteLocalAddress && !addr.isLoopbackAddress) {
                return addr.hostAddress
            }
        }
    }
    return null
}

fun getLanIPs(): List<String> =
    try {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
            .filter { it is Inet4Address && it.isSiteLocalAddress && !it.isLoopbackAddress }
            .map { it.hostAddress }
            .distinct()
            .ifEmpty { listOf("Unknown") }
    } catch (e: Exception) { listOf("Unknown") }

fun getNetworkInterfaces(): List<NetworkInterfaceInfo> =
    try {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && it.hardwareAddress != null }
            .map {
                val mac = it.hardwareAddress?.joinToString(":") { b -> "%02X".format(b) } ?: "Unknown"
                NetworkInterfaceInfo(it.displayName ?: "Unknown", mac)
            }
    } catch (_: Exception) { listOf(NetworkInterfaceInfo("Unknown", "Unknown")) }

fun isIPv6Enabled(): Boolean =
    try {
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .any { addr ->
                addr is Inet6Address &&
                        !addr.isLoopbackAddress &&
                        !addr.isLinkLocalAddress &&
                        !addr.isSiteLocalAddress // site local is deprecated but might be worth excluding
            }
    } catch (e: Exception) { false }

fun gatherInfo(all: Boolean = false): IFetchInfo =
    IFetchInfo(
        timestamp = getTimestamp(),
        hostname = getHostname(),
        osInfo = getOsInfo(),
        externalIPv4 = getExternalIP("https://api.ipify.org"),
        externalIPv6 = getExternalIP("https://api64.ipify.org", true),
        lanIPs = if (all) getAllLanIPs() else getLanIPs(),
        networkInterfaces = if (all) getAllNetworkInterfaces() else getNetworkInterfaces()
    )

fun printSimple(info: IFetchInfo) {
    println("External IP (IPv4): ${info.externalIPv4}")
    println("External IP (IPv6): ${info.externalIPv6}")
    println("LAN IP: ${info.lanIPs.firstOrNull() ?: "Unknown"}")
    println("MAC Address: ${info.networkInterfaces.firstOrNull()?.mac ?: "Unknown"}")
}

fun printHuman(info: IFetchInfo) {
    println("IFetch - System Information @ ${info.timestamp}")
    println("=================================")
    println("Hostname: ${info.hostname}")
    println("OS Info: ${info.osInfo}")
    println()
    println("External IP (IPv4): ${info.externalIPv4}")
    println("External IP (IPv6): ${info.externalIPv6}")
    println()
    println("LAN IPs:")
    info.lanIPs.forEach { println(" - $it") }
    println()
    println("Network Interfaces & MAC Addresses:")
    info.networkInterfaces.forEach { println(" - ${it.name}: ${it.mac}") }
}

fun outputJson(info: IFetchInfo) {
    val gson = GsonBuilder().setPrettyPrinting().create()
    println(gson.toJson(info))
}

fun outputYaml(info: IFetchInfo) {
    val yaml = Yaml()
    println(yaml.encodeToString(info))
}

fun outputCsv(info: IFetchInfo) {
    val fields = listOf(
        info.timestamp, info.hostname, info.osInfo, info.externalIPv4, info.externalIPv6,
        info.lanIPs.joinToString("|"), info.networkInterfaces.joinToString("|") { "${it.name}=${it.mac}" }
    )
    println(fields.joinToString(","))
}

fun outputXml(info: IFetchInfo) {
    val xml = XML { indentString = "  " }
    println(xml.encodeToString(info))
}

fun printHelp(os: String) {
    val isWindows = os.contains("win", ignoreCase = true)
    val dash = if (isWindows) "/" else "-"
    val doubleDash = if (isWindows) "/" else "--"
    val help = if (isWindows) "/help" else "--help"
    val example = if (isWindows) "ifetch /j" else "ifetch -j"
    val serveOption = if (isWindows) "/serve [port]" else "--serve [port]"
    println("""
ifetch [options]

Options:
  ${dash}h, $help              Show help text
  ${dash}s, ${doubleDash}simple            Print minimal LAN/external info
  ${dash}j, ${doubleDash}json              Output data as JSON
  ${dash}y, ${doubleDash}yaml              Output data as YAML
  ${dash}c, ${doubleDash}csv               Output data as CSV
  ${dash}x, ${doubleDash}xml               Output data as XML
      $serveOption         Start API server  (default port 7676)
      ${doubleDash}api-key [key]        Set API key for protected access
      ${doubleDash}rate-limit [amount]  Max requests per IP per minute
      
  No option prints full info in human-readable format.

Examples:
  $example
  ifetch --serve 8080 --api-key secret --rate-limit 5
  
More information is available at https://github.com/Uippao/IFetch
""")
}

fun getAllLanIPs(): List<String> =
    try {
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .filter { it is Inet4Address && !it.isLoopbackAddress }
            .map { it.hostAddress }
            .distinct()
            .ifEmpty { listOf("Unknown") }
    } catch (e: Exception) { listOf("Unknown") }

fun getAllNetworkInterfaces(): List<NetworkInterfaceInfo> =
    try {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.hardwareAddress != null }
            .map {
                val mac = it.hardwareAddress?.joinToString(":") { b -> "%02X".format(b) } ?: "Unknown"
                NetworkInterfaceInfo(it.displayName ?: "Unknown", mac)
            }
    } catch (_: Exception) { listOf(NetworkInterfaceInfo("Unknown", "Unknown")) }

fun main(args: Array<String>) {
    val os = System.getProperty("os.name")
    val isWindows = os.contains("win", ignoreCase = true)
    fun hasOpt(vararg opts: String) = args.any { arg -> opts.any { o -> arg == o } }

    if (hasOpt("-h", "--help", "/h", "/help")) {
        printHelp(os)
        return
    }

    if (args.any { it.startsWith("--serve") || it.startsWith("/serve") }) {
        val serveIdx = args.indexOfFirst { it.startsWith("--serve") || it.startsWith("/serve") }
        val port = if (serveIdx >= 0 && args.size > serveIdx + 1 && args[serveIdx + 1].toIntOrNull() != null)
            args[serveIdx + 1].toInt() else 7676
        val apiKeyIdx = args.indexOfFirst { it == "--api-key" }
        val apiKey = if (apiKeyIdx >= 0 && args.size > apiKeyIdx + 1) args[apiKeyIdx + 1] else null
        val rateIdx = args.indexOfFirst { it == "--rate-limit" }
        val rateLimit = if (rateIdx >= 0 && args.size > rateIdx + 1) args[rateIdx + 1].toIntOrNull() else null
        Server.run(port, apiKey, rateLimit)
        return
    }

    val info = when {
        hasOpt("-s", "--simple", "/s", "/simple") -> gatherInfo(false)
        hasOpt("-j", "--json", "/j", "/json") -> gatherInfo(true)
        hasOpt("-y", "--yaml", "/y", "/yaml") -> gatherInfo(true)
        hasOpt("-c", "--csv", "/c", "/csv") -> gatherInfo(true)
        hasOpt("-x", "--xml", "/x", "/xml") -> gatherInfo(true)
        else -> gatherInfo(true)
    }

    when {
        hasOpt("-s", "--simple", "/s", "/simple") -> printSimple(info)
        hasOpt("-j", "--json", "/j", "/json") -> outputJson(info)
        hasOpt("-y", "--yaml", "/y", "/yaml") -> outputYaml(info)
        hasOpt("-c", "--csv", "/c", "/csv") -> outputCsv(info)
        hasOpt("-x", "--xml", "/x", "/xml") -> outputXml(info)
        else -> printHuman(info)
    }
}