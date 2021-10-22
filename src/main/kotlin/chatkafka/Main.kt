package chatkafka

import kotlinx.html.*
import kotlinx.html.dom.*
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.w3c.dom.Document
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kafka.sender.SenderRecord
import java.time.Duration


@SpringBootApplication
@RestController
class Main {

    @GetMapping("/")
    fun index(): String {
        return Html.index.serialize(true)
    }

}

data class BootstrapServers(val stringList: String)

@Configuration
class WebSocketConfig {

    @Bean
    fun simpleUrlHandlerMapping(bootstrapServers: BootstrapServers): SimpleUrlHandlerMapping {
        return SimpleUrlHandlerMapping(mapOf("/chat" to chat(bootstrapServers)), 0)
    }

    fun chat(bootstrapServers: BootstrapServers): WebSocketHandler {

        val consumerProps = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers.stringList,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.GROUP_ID_CONFIG to "group",
        )

        val producerProps = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers.stringList,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        )

        return WebSocketHandler { session ->
            val receiverOptions = ReceiverOptions.create<String, String>(consumerProps)
                .consumerProperty(ConsumerConfig.CLIENT_ID_CONFIG, session.id)
                .subscription(listOf("chat_out"))

            val kafkaReceiver = KafkaReceiver.create(receiverOptions).receive()

            val kafkaToClient = kafkaReceiver.map { session.textMessage(it.value()) }


            val senderOptions = SenderOptions.create<String, String>(producerProps)

            val kafkaSender = KafkaSender.create(senderOptions)

            val clientToRecord = session.receive().map { SenderRecord.create(ProducerRecord("chat_in", session.id, it.payloadAsText), null) }

            val clientToKafka = kafkaSender.send(clientToRecord)


            // todo: do we need to cancel / close the kafka stuff when the websocket closes?
            session.send(kafkaToClient).and(clientToKafka)
        }
    }

    @Bean
    fun webSocketHandlerAdapter(): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter()
    }

}

object Html {

    private val indexHTML: HTML.() -> Unit = {
        head {
            link("/webjars/bootstrap/4.5.3/css/bootstrap.min.css", LinkRel.stylesheet)
            link("/index.css", LinkRel.stylesheet)
            script(ScriptType.textJavaScript) {
                src = "/index.js"
            }
        }
        body {
            nav("navbar fixed-top navbar-light bg-light") {
                a("/", classes = "navbar-brand") {
                    +"Chat Kafka"
                }
            }

            div("container-fluid") {
                ul {
                    id = "chats"
                }
            }

            form(classes = "form-inline") {
                div(classes = "input-group") {
                    input(InputType.text, classes = "form-control") {
                        id = "message"
                    }
                }

                button(type = ButtonType.submit, classes = "btn btn-primary") {
                    id = "send"
                    +"Send"
                }
            }

        }
    }

    val index: Document = createHTMLDocument().html(block = indexHTML)

}

fun main(args: Array<String>) {
    runApplication<Main>(*args)
}