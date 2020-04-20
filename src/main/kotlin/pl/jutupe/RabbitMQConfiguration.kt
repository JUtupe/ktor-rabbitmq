package pl.jutupe

import com.rabbitmq.client.Channel
import kotlin.reflect.KClass

class RabbitMQConfiguration private constructor() {

    internal lateinit var uri: String
    internal var connectionName: String? = null

    internal lateinit var initializeBlock: (Channel.() -> Unit)

    lateinit var serializeBlock:  (Any) -> ByteArray
    lateinit var deserializeBlock: (ByteArray, KClass<*>) -> Any

    /**
     * @param [block] invoked with [Channel] in order to initialize rabbit
     * (create exchange, queue, etc.)
     */
    internal fun initialize(block: (Channel.() -> Unit)) {
        initializeBlock = block
    }

    /**
     * @param [block] used for message body serialization.
     */
    internal fun serialize(block: (Any) -> ByteArray) {
        serializeBlock = block
    }

    /**
     * @param [block] used for message body deserialization.
     */
    internal fun deserialize(block: (ByteArray, KClass<*>) -> Any) {
        deserializeBlock = block
    }

    companion object {
        fun create(): RabbitMQConfiguration {
            return RabbitMQConfiguration()
        }
    }
}