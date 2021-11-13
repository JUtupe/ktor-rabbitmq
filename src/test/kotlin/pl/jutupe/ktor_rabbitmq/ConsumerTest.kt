package pl.jutupe.ktor_rabbitmq

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import io.ktor.server.testing.*
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

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

class ConsumerTest : IntegrationTest() {

    @Test
    fun `should consume message when published`() {
        val consumer = mockk<(String, TestObject) -> Unit>()

        withTestApplication({
            testModule(rabbit.host, rabbit.amqpPort)

            rabbitConsumer {
                consume("queue", true, consumer)
            }
        }) {
            // given
            val body = TestObject("value")
            val convertedBody = jacksonObjectMapper().writeValueAsBytes(body)

            // when
            withChannel {
                basicPublish("exchange", "routingKey", null, convertedBody)
            }

            // then
            verify { consumer.invoke(any(), eq(body))}
        }
    }
}