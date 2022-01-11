package pl.jutupe.ktor_rabbitmq

import com.rabbitmq.client.Channel
import io.ktor.application.Application
import io.ktor.application.log
import org.slf4j.Logger
import kotlin.reflect.KClass

class RabbitMQConfiguration private constructor() {

    lateinit var uri: String
    var rabbitMQInstance: RabbitMQInstance? = null
    var connectionName: String? = null
    var logger: Logger? = null
        private set

    internal lateinit var initializeBlock: (Channel.() -> Unit)

    lateinit var serializeBlock:  (Any) -> ByteArray
    lateinit var deserializeBlock: (ByteArray, KClass<*>) -> Any

    /**
     * Enables logging by passing [Application.log] into [RabbitMQ]
     */
    fun Application.enableLogging() {
        logger = log
    }

    /**
     * @param [block] invoked with [Channel] in order to initialize rabbit
     * (create exchange, queue, etc.)
     */
    fun initialize(block: (Channel.() -> Unit)) {
        initializeBlock = block
    }

    /**
     * @param [block] used for message body serialization.
     */
    fun serialize(block: (Any) -> ByteArray) {
        serializeBlock = block
    }

    /**
     * @param [block] used for message body deserialization.
     */
    fun deserialize(block: (ByteArray, KClass<*>) -> Any) {
        deserializeBlock = block
    }

    companion object {
        fun create(): RabbitMQConfiguration {
            return RabbitMQConfiguration()
        }
    }
}
