package io.wisoft

import io.ktor.application.*
import io.ktor.application.install
import io.ktor.client.request.request
import io.ktor.http.cio.websocket.pingPeriod
import io.ktor.http.cio.websocket.timeout
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.websocket.WebSockets
import io.rsocket.kotlin.RSocketRequestHandler
import io.rsocket.kotlin.core.RSocketServer
import io.rsocket.kotlin.payload.Payload
import io.rsocket.kotlin.payload.buildPayload
import io.rsocket.kotlin.payload.data
import io.rsocket.kotlin.payload.metadata
import io.rsocket.kotlin.transport.ktor.server.RSocketSupport
import io.rsocket.kotlin.transport.ktor.server.rSocket
import java.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

fun main() {
    val rSocketServer = RSocketServer()
    embeddedServer(CIO, port = 9000) {
        install(WebSockets)
        install(RSocketSupport) {
            server = rSocketServer
        }
        routing {
            rSocket("/") {
                RSocketRequestHandler {
                    fireAndForget { request: Payload ->
                        println(request.data.readText())
                    }
                    requestResponse { request: Payload ->
                        buildPayload {
                            data("Response data")
                            metadata("Response metadata")
                        }
                    }
                    requestStream { request: Payload ->
                        flow {
                            repeat(10) {
                                emit(buildPayload { data("Stream response: $it") })
                            }
                        }
                    }
                    requestChannel { initPayload, payloads ->
                        flow {
                            repeat(10) {
                                emit(buildPayload { data("Channel response: $it") })
                            }
                        }
                    }
                    metadataPush { request: ByteReadPacket ->
                        println("Request: ${request.readText()}")
                    }
                }
            }
        }
    }.start(wait = true)
}
