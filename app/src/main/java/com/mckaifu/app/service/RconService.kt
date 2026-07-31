package com.mckaifu.app.service

import java.io.*
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

class RconService(private val host: String, private val port: Int, private val password: String) {

    private var socket: Socket? = null
    private var output: DataOutputStream? = null
    private var input: DataInputStream? = null
    private var requestId = 0

    companion object {
        private const val TYPE_AUTH = 3
        private const val TYPE_COMMAND = 2
        private const val TYPE_RESPONSE = 0
    }

    fun connect(): Boolean {
        return try {
            socket = Socket(host, port)
            socket?.soTimeout = 5000
            output = DataOutputStream(socket?.getOutputStream())
            input = DataInputStream(socket?.getInputStream())
            authenticate()
        } catch (e: Exception) {
            false
        }
    }

    private fun authenticate(): Boolean {
        val response = sendPacket(TYPE_AUTH, password)
        return response?.type == TYPE_AUTH && response?.requestId != -1
    }

    fun sendCommand(command: String): String {
        val response = sendPacket(TYPE_COMMAND, command)
        return response?.body ?: ""
    }

    private data class Packet(val requestId: Int, val type: Int, val body: String)

    private fun sendPacket(type: Int, body: String): Packet? {
        return try {
            val id = ++requestId
            val bodyBytes = body.toByteArray(Charsets.UTF_8)
            val length = 4 + 4 + bodyBytes.size + 2
            val buffer = ByteBuffer.allocate(length + 4)
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            buffer.putInt(length)
            buffer.putInt(id)
            buffer.putInt(type)
            buffer.put(bodyBytes)
            buffer.put(0.toByte())
            buffer.put(0.toByte())

            output?.write(buffer.array())
            output?.flush()

            readResponse()
        } catch (e: Exception) {
            null
        }
    }

    private fun readResponse(): Packet? {
        return try {
            val stream = input ?: return null
            val lengthBytes = ByteArray(4)
            stream.readFully(lengthBytes)
            val length = ByteBuffer.wrap(lengthBytes)
                .order(ByteOrder.LITTLE_ENDIAN).getInt()

            val idBytes = ByteArray(4)
            stream.readFully(idBytes)
            val id = ByteBuffer.wrap(idBytes)
                .order(ByteOrder.LITTLE_ENDIAN).getInt()

            val typeBytes = ByteArray(4)
            stream.readFully(typeBytes)
            val type = ByteBuffer.wrap(typeBytes)
                .order(ByteOrder.LITTLE_ENDIAN).getInt()

            val bodyBytes = ByteArray(length - 10)
            stream.readFully(bodyBytes)
            val body = String(bodyBytes, Charsets.UTF_8).trimEnd('\u0000')
            Packet(id, type, body)
        } catch (e: Exception) {
            null
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (_: Exception) {}
    }
}
