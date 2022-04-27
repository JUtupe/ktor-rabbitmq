package pl.jutupe.ktor_rabbitmq

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.slf4j.Logger

private fun Application.testModule(host: String, port: Int) {
    install(RabbitMQ) {
        uri = "amqp://guest:guest@$host:$port"
        connectionName = "Connection name"

        enableLogging()

        serialize { jacksonObjectMapper().writeValueAsBytes(it) }
        deserialize { bytes, type -> jacksonObjectMapper().readValue(bytes, type.javaObjectType) }

        initialize {
            exchangeDeclare("exchange", "direct", true)
            queueDeclare("queue", true, false, false, emptyMap())
            queueBind("queue", "exchange", "routingKey")
        }
    }
}

class ConsumerTest : IntegrationTest() {

    @Test
    fun `should consume message when published`() {
        val consumer = mockk<ConsumerScope.(TestObject) -> Unit>()

        testApplication {
            application {
                testModule(rabbit.host, rabbit.amqpPort)

                rabbitConsumer {
                    consume("queue", true, rabbitDeliverCallback = consumer)
                }

                // given
                val body = TestObject("value")
                val convertedBody = jacksonObjectMapper().writeValueAsBytes(body)

                // when
                withChannel {
                    basicPublish("exchange", "routingKey", null, convertedBody)
                }

                // then
                verify { consumer.invoke(any(), eq(body)) }
            }
        }
    }

    @Test
    fun `should consume message when published using precreated RabbitMQ`() {
        val consumer = mockk<ConsumerScope.(TestObject) -> Unit>()

        testApplication {
            application {
                install(RabbitMQ) {
                    rabbitMQInstance = RabbitMQInstance(
                        RabbitMQConfiguration.create()
                            .apply {
                                uri = "amqp://guest:guest@${rabbit.host}:${rabbit.amqpPort}"
                                connectionName = "Connection name"

                                serialize { jacksonObjectMapper().writeValueAsBytes(it) }
                                deserialize { bytes, type ->
                                    jacksonObjectMapper().readValue(
                                        bytes,
                                        type.javaObjectType
                                    )
                                }

                                initialize {
                                    exchangeDeclare("exchange", "direct", true)
                                    queueDeclare("queue", true, false, false, emptyMap())
                                    queueBind("queue", "exchange", "routingKey")
                                }
                            }
                    )
                }

                rabbitConsumer {
                    consume("queue", true, rabbitDeliverCallback = consumer)
                }

                // given
                val body = TestObject("value")
                val convertedBody = jacksonObjectMapper().writeValueAsBytes(body)

                // when
                withChannel {
                    basicPublish("exchange", "routingKey", null, convertedBody)
                }

                // then
                verify { consumer.invoke(any(), eq(body)) }
            }
        }
    }

    @Test
    fun `should log error when invalid body published`() {
        val consumer = mockk<ConsumerScope.(TestObject) -> Unit>()
        val logger = mockk<Logger>(relaxUnitFun = true)

        testApplication {
            // given
            environment {
                this.log = logger
            }
            application {
                testModule(rabbit.host, rabbit.amqpPort)

                rabbitConsumer {
                    consume("queue", true, rabbitDeliverCallback = consumer)
                }

                val body = AnotherTestObject(string = "string", int = 1234)
                val convertedBody = jacksonObjectMapper().writeValueAsBytes(body)

                // when
                withChannel {
                    basicPublish("exchange", "routingKey", null, convertedBody)
                }

                // then
                verify { logger.error(any(), any<Throwable>()) }
                verify(exactly = 0) { consumer.invoke(any(), any()) }
            }
        }
    }
}
