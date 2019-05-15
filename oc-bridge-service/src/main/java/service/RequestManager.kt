package service

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.web.RoutingContext
import org.yaml.snakeyaml.Yaml

import java.util.UUID

object RequestManager {
    private var vertx: Vertx? = null
    private val config = JsonObject()
    private const val DB_CONNECTION_STRING = "connection_string"
    private const val DOCUMENT_ID = "_id"
    private const val EVENT_ID = "eventId"
    private const val COLLECTION_NAME = "events"
    private const val DUPLICATED_KEY_CODE = "E11000"


    fun initializeRequestManager(vertx: Vertx) {
        RequestManager.vertx = vertx

        val yaml = Yaml()
        val inputStream = RequestManager::class.java
                .classLoader
                .getResourceAsStream("mongoConfig.yaml")
        val obj = yaml.load<Map<String, Any>>(inputStream)
        config.put(DB_CONNECTION_STRING, obj[DB_CONNECTION_STRING])
    }

    fun handleCreateEvent(routingContext: RoutingContext) {
        val response = routingContext.response()
        val uuid = UUID.randomUUID().toString()
        val document = JsonObject().put(DOCUMENT_ID, uuid)

        MongoClient.createNonShared(vertx, config).insert(COLLECTION_NAME, document) { result ->
            when {
                result.succeeded() -> response
                        .putHeader("Content-Type", "application/json")
                        .setStatusCode(HttpResponseStatus.CREATED.code())
                        .end(Json.encodePrettily(document))
                isDuplicateKey(result.cause().message) -> handleCreateEvent(routingContext)
                else -> response.setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end()
            }
        }
    }

    private fun isDuplicateKey(errorMessage: String?) = errorMessage?.startsWith(DUPLICATED_KEY_CODE) ?: false

    fun handleGetEventById(routingContext: RoutingContext) {
        val response = routingContext.response()
        val eventId = routingContext.request().getParam(EVENT_ID)
        try {
            UUID.fromString(eventId)
            val query = JsonObject().put(DOCUMENT_ID, eventId)
            MongoClient.createNonShared(vertx, config).find(COLLECTION_NAME, query) { result ->
                if (result.succeeded()) {
                    try {
                        val resultJson = result.result()[0]
                        resultJson.remove(DOCUMENT_ID)
                        if (resultJson.size() > 0)
                            response
                                    .putHeader("Content-Type", "application/json")
                                    .setStatusCode(HttpResponseStatus.OK.code())
                                    .end(Json.encodePrettily(resultJson))
                        else
                            response.setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end()
                    } catch (ex: IndexOutOfBoundsException) {
                        response.setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end()
                    }

                } else {
                    response.setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end()
                }
            }
        } catch (exception: IllegalArgumentException) {
            response.setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end()
        }

    }

    fun handleUpdateEvent(routingContext: RoutingContext) {
        val response = routingContext.response()
        val body = routingContext.bodyAsJson
        val query = JsonObject().put(DOCUMENT_ID, routingContext.request().getParam(EVENT_ID))
        val update = JsonObject().put("\$set", body)
        MongoClient.createNonShared(vertx, config).updateCollection(COLLECTION_NAME, query, update) { res ->
            if (res.succeeded()) {
                when {
                    res.result().docModified == 0L -> response.setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end()
                    else -> response.setStatusCode(HttpResponseStatus.NO_CONTENT.code()).end()
                }
            } else {
                response.setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end()
            }
        }
    }
}
