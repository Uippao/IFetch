package me.uippao.ifetch

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import com.google.gson.GsonBuilder
import com.charleskorn.kaml.Yaml
import io.ktor.server.plugins.origin
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.serialization.XML

object Server {
    private val ipRequestLog = ConcurrentHashMap<String, CopyOnWriteArrayList<Long>>()

    fun run(port: Int, apiKey: String?, rateLimit: Int?) {
        embeddedServer(Netty, port = port) {
            routing {
                get("/") {
                    call.respondText("IFetch API server is running\n", ContentType.Text.Plain)
                }

                get("/info") {
                    if (!checkApiKey(call, apiKey)) return@get
                    if (!checkRate(call, rateLimit)) return@get
                    call.respondText(humanInfo(), ContentType.Text.Plain)
                }

                get("/info/simple") {
                    if (!checkApiKey(call, apiKey)) return@get
                    if (!checkRate(call, rateLimit)) return@get
                    call.respondText(simpleInfo(), ContentType.Text.Plain)
                }

                get("/info/json") {
                    if (!checkApiKey(call, apiKey)) return@get
                    if (!checkRate(call, rateLimit)) return@get
                    call.respondText(jsonInfo(), ContentType.Application.Json)
                }

                get("/info/yaml") {
                    if (!checkApiKey(call, apiKey)) return@get
                    if (!checkRate(call, rateLimit)) return@get
                    call.respondText(yamlInfo(), ContentType.Text.Plain)
                }

                get("/info/csv") {
                    if (!checkApiKey(call, apiKey)) return@get
                    if (!checkRate(call, rateLimit)) return@get
                    call.respondText(csvInfo(), ContentType.Text.CSV)
                }

                get("/info/xml") {
                    if (!checkApiKey(call, apiKey)) return@get
                    if (!checkRate(call, rateLimit)) return@get
                    call.respondText(xmlInfo(), ContentType.Text.Xml)
                }
            }
        }.start(wait = true)
    }

    // Existing checkApiKey and checkRate functions remain unchanged

    private fun humanInfo(): String = buildString {
        val info = gatherInfo(all = true)
        append("IFetch - System Information @ ${info.timestamp}\n")
        append("=================================\n")
        append("Hostname: ${info.hostname}\n")
        append("OS Info: ${info.osInfo}\n\n")
        append("External IP (IPv4): ${info.externalIPv4}\n")
        append("External IP (IPv6): ${info.externalIPv6}\n\n")
        append("LAN IPs:\n")
        info.lanIPs.forEach { append(" - $it\n") }
        append("\nNetwork Interfaces & MAC Addresses:\n")
        info.networkInterfaces.forEach { append(" - ${it.name}: ${it.mac}\n") }
    }

    private fun simpleInfo(): String = buildString {
        val info = gatherInfo(all = false)
        append("External IP (IPv4): ${info.externalIPv4}\n")
        append("External IP (IPv6): ${info.externalIPv6}\n")
        append("LAN IP: ${info.lanIPs.firstOrNull() ?: "Unknown"}\n")
        append("MAC Address: ${info.networkInterfaces.firstOrNull()?.mac ?: "Unknown"}\n")
    }

    private fun jsonInfo(): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(gatherInfo(all = true))
    }

    private fun yamlInfo(): String {
        val yaml = Yaml()
        return yaml.encodeToString(gatherInfo(all = true))
    }

    private fun csvInfo(): String {
        val info = gatherInfo(all = true)
        return listOf(
            info.timestamp, info.hostname, info.osInfo,
            info.externalIPv4, info.externalIPv6,
            info.lanIPs.joinToString("|"),
            info.networkInterfaces.joinToString("|") { "${it.name}=${it.mac}" }
        ).joinToString(",")
    }

    private fun xmlInfo(): String {
        val xml = XML { indentString = "  " }
        return xml.encodeToString(gatherInfo(all = true))
    }

    private suspend fun checkApiKey(call: ApplicationCall, apiKey: String?): Boolean {
        if (apiKey == null) return true
        val header = call.request.headers["X-API-Key"]
        if (header == apiKey) return true
        call.respond(HttpStatusCode.Forbidden, "403 Forbidden: Invalid or missing API key.\n")
        return false
    }

    private suspend fun checkRate(call: ApplicationCall, limit: Int?): Boolean {
        if (limit == null) return true
        val ip = call.request.origin.remoteHost
        val now = Instant.now().epochSecond
        val windowStart = now - 60
        val list = ipRequestLog.computeIfAbsent(ip) { CopyOnWriteArrayList() }
        list.removeIf { it < windowStart }
        if (list.size >= limit) {
            call.respond(HttpStatusCode.TooManyRequests, "429 Too Many Requests: Rate limit exceeded.\n")
            return false
        }
        list.add(now)
        return true
    }
}