package pl.jutupe.ktor_rabbitmq

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
open class IntegrationTest {

    @field:Container
    protected val rabbit = RabbitMQContainer(DockerImageName.parse("rabbitmq"))

    protected fun verifyMessages(queue: String, routingKey: String, messages: List<String>) {
        val readMessages = mutableListOf<String>()

        withChannel {
            while (true) {
                val response = basicGet(queue, true) ?: break

                val envelope = response.envelope

                assertEquals(routingKey, envelope.routingKey)
                readMessages += String(response.body)
            }
        }

        assertEquals(messages, readMessages)
    }

    protected fun <T> withChannel(block: Channel.() -> T): T {
        val connection = ConnectionFactory().apply {
            host = rabbit.host
            port = rabbit.amqpPort
        }.newConnection()
        val channel = connection.createChannel()

        return block(channel).apply {
            channel.close()
            connection.close()
        }
    }

    data class TestObject(val key: String)

    data class AnotherTestObject(val string: String, val int: Int)
}