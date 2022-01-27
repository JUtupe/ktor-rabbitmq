# ktor-rabbitmq

[![](https://jitpack.io/v/JUtupe/ktor-rabbitmq.svg)](https://jitpack.io/#JUtupe/ktor-rabbitmq)

## Show me the code!

```kotlin
install(RabbitMQ) {
    uri = "amqp://guest:guest@localhost:5672"
    connectionName = "Connection name"

    //serialize and deserialize functions are required
    serialize { jacksonObjectMapper().writeValueAsBytes(it) }
    deserialize { bytes, type -> jacksonObjectMapper().readValue(bytes, type.javaObjectType) }

    //example initialization logic
    initialize {
        exchangeDeclare(/* exchange = */ "exchange", /* type = */ "direct", /* durable = */ true)
        queueDeclare(
            /* queue = */ "queue",
            /* durable = */true,
            /* exclusive = */false,
            /* autoDelete = */false,
            /* arguments = */emptyMap()
        )
        queueBind(/* queue = */ "queue", /* exchange = */ "exchange", /* routingKey = */ "routingKey")
    }
}

//publish example
routing {
    get("anyEndpoint") {
        call.publish("exchange", "routingKey", null, MyObject("test name"))
    }
}

//consume with autoack example
rabbitConsumer {
    consume<MyObject>("queue") { body ->
        println("Consumed message $body")
    }
}
```

## Documentation and setup guide

You can find it [here](https://github.com/JUtupe/ktor-rabbitmq/wiki)
