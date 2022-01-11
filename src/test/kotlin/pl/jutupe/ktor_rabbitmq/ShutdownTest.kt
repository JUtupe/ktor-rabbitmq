package pl.jutupe.ktor_rabbitmq

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.server.testing.createTestEnvironment
import io.ktor.server.testing.withApplication
import io.ktor.server.testing.withTestApplication
import io.mockk.*
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private const val SHUTDOWN_TIMEOUT = 1234L

private fun Application.testModule(host: String, port: Int) {
    install(RabbitMQ) {
        uri = "amqp://guest:guest@$host:$port"
        connectionName = "Connection name"
        shutdownTimeout = SHUTDOWN_TIMEOUT

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

class ShutdownTest : IntegrationTest() {

    @Test
    fun `should shutdown with configuration timeout`() {
        val logger = mockk<Logger>(relaxUnitFun = true)

        mockkConstructor(ThreadPoolExecutor::class)
        every { anyConstructed<ThreadPoolExecutor>().awaitTermination(SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS) } returns true

        withApplication( createTestEnvironment { this.log = logger } ) {
            // given
            application.apply {
                testModule(rabbit.host, rabbit.amqpPort)
            }
        }

        // then
        verifyOrder {
            logger.info("RabbitMQ initializing")
            logger.info("RabbitMQ initialized")
            logger.info("RabbitMQ stopping")
            logger.info("RabbitMQ stopped")
        }
    }

    @Test
    fun `should throw on shutdown with long running operation`() {
        val logger = mockk<Logger>(relaxUnitFun = true)
        val testValue = "test"
        val convertedBody = jacksonObjectMapper().writeValueAsBytes(testValue)

        withApplication(createTestEnvironment { this.log = logger }) {
            // given
            application.apply {
                testModule(rabbit.host, rabbit.amqpPort)

                rabbitConsumer {
                    consume<String>("queue", false) {
                        log.info("consuming")
                        Thread.sleep(20_000)
                        ack()
                        log.info("acked")
                    }
                }
            }

            // when
            withChannel {
                basicPublish("exchange", "routingKey", null, convertedBody)
            }
        }

        // then
        verifyOrder {
            logger.info("RabbitMQ initializing")
            logger.info("RabbitMQ initialized")
            logger.info("RabbitMQ stopping")
            logger.info("consuming")
            logger.info("RabbitMQ stopped")
        }
        verify(exactly = 0) {
            logger.info("acked")
        }
        withTestApplication({ testModule(rabbit.host, rabbit.amqpPort) }) {
            verifyMessages("queue", "routingKey", listOf("test"))
        }
    }
}