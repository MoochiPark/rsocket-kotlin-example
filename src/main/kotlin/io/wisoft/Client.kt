package io.wisoft.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.websocket.WebSockets
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeDouble
import io.ktor.utils.io.core.writeFloat
import io.ktor.utils.io.core.writeInt
import io.ktor.utils.io.core.writeText
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.payload.Payload
import io.rsocket.kotlin.payload.buildPayload
import io.rsocket.kotlin.payload.data
import io.rsocket.kotlin.payload.metadata
import io.rsocket.kotlin.transport.ktor.client.RSocketSupport
import io.rsocket.kotlin.transport.ktor.client.rSocket
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

val client = HttpClient(CIO) {
    install(WebSockets)
    install(RSocketSupport)
//        connector = RSocketConnector {
//            connectionConfig {
//                keepAlive = KeepAlive(
//                    interval = 30.seconds,
//                    maxLifetime = 2.minutes
//                )
//
//                setupPayload { buildPayload { data("hello world") } }
//
//                payloadMimeType = PayloadMimeType(
//                    data = "application/json",
//                    metadata = "application/json"
//                )
//            }
//
//            acceptor {
//                RSocketRequestHandler {
//                    requestResponse { it }
//                }
//            }
//        }

}


fun main() = runBlocking {
    val rSocket: RSocket = client.rSocket("ws://localhost:9000/")

    println("Fire and Forget:")
    rSocket.fireAndForget(buildPayload { data("Fire and Forget data") })

    println("Request-Response:")
    val response: Payload = rSocket.requestResponse(buildPayload { data("Request data") })
    println("Response data: ${response.data.readText()} ")

    println("Request-Stream:")
    val stream: Flow<Payload> = rSocket.requestStream(buildPayload { data("Request-Stream data") })
    stream.collect { payload: Payload ->
        println(payload.data.readText())
    }

    println("Request-Channel:")
    val channelStream: Flow<Payload> = rSocket.requestChannel(
        initPayload = Payload.Empty,
        payloads = flow {
            repeat(10) {
                emit(buildPayload { data("Channel request: $it") })
            }
        })
    channelStream.collect { payload: Payload ->
        println(payload.data.readText())
    }

    println("Metadata-Push")
    rSocket.metadataPush(buildPacket {
        writeText("Push text")
    })
}
