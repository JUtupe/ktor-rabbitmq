package pl.jutupe.ktor_rabbitmq

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.io.IOException

private fun Application.testModule(host: String, port: Int) {
    install(RabbitMQ) {
        uri = "amqp://guest:guest@$host:$port"
        connectionName = "Connection name"

        serialize { jacksonObjectMapper().writeValueAsBytes(it) }
        deserialize { bytes, type -> jacksonObjectMapper().readValue(bytes, type.javaObjectType) }

        initialize {
            exchangeDeclare("exchange", "direct", true)
            queueDeclare("queue", true, false, false, emptyMap())
            queueBind("queue", "exchange", "routingKey")
        }
    }
}

class InitializeTest : IntegrationTest() {

    @Test
    fun `should create queue when declared in initialize block`(): Unit =
        testApplication {
            application {
                testModule(rabbit.host, rabbit.amqpPort)

                // when
                assertDoesNotThrow {
                    withChannel {
                        basicGet("queue", true)
                    }
                }
            }
        }

    @Test
    fun `should throw when queue not created in initialize block`(): Unit =
        testApplication{
            application {
                testModule(rabbit.host, rabbit.amqpPort)

                // when
                assertThrows<IOException> {
                    withChannel {
                        basicGet("queue1", true)
                    }
                }
            }
        }

    @Test
    fun `should support passing pre-initialized instance of RabbitMQ`(): Unit =
        testApplication {
            application {
                install(RabbitMQ) {
                    rabbitMQInstance = RabbitMQInstance(
                        RabbitMQConfiguration.create()
                            .apply {
                                uri = "amqp://guest:guest@${rabbit.host}:${rabbit.amqpPort}"
                                connectionName = "Connection name"

                                serialize { jacksonObjectMapper().writeValueAsBytes(it) }
                                deserialize { bytes, type -> jacksonObjectMapper().readValue(bytes, type.javaObjectType) }

                                initialize {
                                    exchangeDeclare("exchange", "direct", true)
                                    queueDeclare("queue", true, false, false, emptyMap())
                                    queueBind("queue", "exchange", "routingKey")
                                }
                            }
                    )
                }

                // when
                assertDoesNotThrow {
                    withChannel {
                        basicGet("queue", true)
                    }
                }
            }
        }
}
