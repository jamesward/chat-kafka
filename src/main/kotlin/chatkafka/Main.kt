package chatkafka

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.core.v1.CloudEventBuilder
import io.cloudevents.kafka.CloudEventDeserializer
import io.cloudevents.kafka.CloudEventSerializer
import kotlinx.html.ButtonType
import kotlinx.html.HTML
import kotlinx.html.InputType
import kotlinx.html.LinkRel
import kotlinx.html.ScriptType
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.dom.serialize
import kotlinx.html.form
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.link
import kotlinx.html.nav
import kotlinx.html.script
import kotlinx.html.ul
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.w3c.dom.Document
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kafka.sender.SenderRecord
import java.util.*


@SpringBootApplication
@RestController
class Main {

    @GetMapping("/")
    fun index(): String {
        return Html.index.serialize(true)
    }

}

data class BootstrapServers(val stringList: String)

data class Text(val body: String)

@Configuration
class WebSocketConfig {

    @Bean
    @ConditionalOnProperty(name = ["kafka.bootstrap.servers"])
    fun bootstrapServers(@Value("\${kafka.bootstrap.servers}") bootstrapServers: String): BootstrapServers {
        return BootstrapServers(bootstrapServers)
    }

    @Bean
    fun simpleUrlHandlerMapping(bootstrapServers: BootstrapServers, objectMapper: ObjectMapper): SimpleUrlHandlerMapping {
        return SimpleUrlHandlerMapping(mapOf("/chat" to chat(bootstrapServers, objectMapper)), 0)
    }

    fun chat(bootstrapServers: BootstrapServers, objectMapper: ObjectMapper): WebSocketHandler {
        return WebSocketHandler { session ->
            val consumerProps = mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers.stringList,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to CloudEventDeserializer::class.java,
                ConsumerConfig.GROUP_ID_CONFIG to session.id,
            )

            val producerProps = mapOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers.stringList,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to CloudEventSerializer::class.java,
            )

            val receiverOptions = ReceiverOptions.create<String, CloudEvent>(consumerProps)
                .subscription(listOf("chat_out"))

            val kafkaReceiver = KafkaReceiver.create(receiverOptions).receive()

            val kafkaToClient = kafkaReceiver.mapNotNull {
                it.value().data?.toBytes()?.let { data ->
                    session.textMessage(String(data))
                }
            }

            val senderOptions = SenderOptions.create<String, CloudEvent>(producerProps)

            val kafkaSender = KafkaSender.create(senderOptions)

            val clientToRecord = session.receive().mapNotNull {
                val event = CloudEventBuilder()
                    .withId(UUID.randomUUID().toString())
                    .withType("chat")
                    .withSource(session.handshakeInfo.uri)
                    .withData(it.payloadAsText.toByteArray())
                    .build()

                SenderRecord.create(ProducerRecord("chat_in", session.id, event), null)
            }

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