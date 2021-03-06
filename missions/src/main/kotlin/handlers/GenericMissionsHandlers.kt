package handlers

import java.util.UUID
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.web.RoutingContext
import io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST
import io.netty.handler.codec.http.HttpResponseStatus.CREATED
import io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR
import io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND
import io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT
import io.netty.handler.codec.http.HttpResponseStatus.OK
import handlers.Shared.MISSIONS_COLLECTION
import handlers.Shared.FAILED_VALIDATION_MESSAGE
import handlers.Shared.MONGODB_CONFIGURATION

object GenericMissionsHandlers {

    fun createMission(context: RoutingContext) {
        val response = context.response()
        val requestBody = context.bodyAsJson

        val missionId = UUID.randomUUID()
        requestBody.put("_id", missionId.toString())
                .put("ongoing", true)
        MongoClient.createNonShared(Main.vertx, MONGODB_CONFIGURATION)
                .save(MISSIONS_COLLECTION, requestBody) { saveOperation ->
                    when {
                        saveOperation.failed() && saveOperation.cause().message == FAILED_VALIDATION_MESSAGE ->
                            response.setStatusCode(BAD_REQUEST.code()).end()
                        saveOperation.failed() ->
                            response.setStatusCode(INTERNAL_SERVER_ERROR.code()).end()
                        else -> {
                            val id = missionId.toString()
                            val uri = context.request().absoluteURI().plus("/$id")
                            response.putHeader("Content-Type", "text/plain")
                                    .putHeader("Location", uri)
                                    .setStatusCode(CREATED.code())
                                    .end(id)
                        }
                    }
                }
    }

    fun retrieveMissions(context: RoutingContext) {
        val response = context.response()
        val params = context.request().params()

        val query = json { obj() }
        params["eventId"]?.let { query.put("eventId", it) }
        params["vehicle"]?.let { query.put("vehicle", it) }
        params["ongoing"]?.toBoolean()?.let { query.put("ongoing", it) }
        MongoClient.createNonShared(Main.vertx, MONGODB_CONFIGURATION)
                .find(MISSIONS_COLLECTION, query) { findOperation ->
                    if (findOperation.failed()) {
                        response.setStatusCode(INTERNAL_SERVER_ERROR.code()).end()
                    } else {
                        val results: List<JsonObject> = findOperation.result()
                        if (results.isEmpty()) {
                            response.setStatusCode(NO_CONTENT.code()).end()
                        } else {
                            val ids: List<String> = results.map { result ->
                                result.getString("_id")
                            }
                            val links: List<String> = ids.map { id ->
                                context.request().absoluteURI().plus(id)
                            }
                            val resultsAndLinks: JsonObject = json { obj(
                                    "ids" to ids,
                                    "links" to links
                            ) }
                            response.putHeader("Content-Type", "application/json")
                                    .setStatusCode(OK.code())
                                    .end(Json.encodePrettily(resultsAndLinks))
                        }
                    }
                }
    }

    fun retrieveMission(context: RoutingContext) {
        val response = context.response()
        val missionId: String? = context.request().getParam("missionId")

        val query = json { obj("_id" to missionId) }
        MongoClient.createNonShared(Main.vertx, MONGODB_CONFIGURATION)
                .findOne(MISSIONS_COLLECTION, query, null) { findOneOperation ->
                    if (findOneOperation.failed()) {
                        response.setStatusCode(INTERNAL_SERVER_ERROR.code()).end()
                    } else {
                        val result: JsonObject? = findOneOperation.result()
                        if (result == null) {
                            response.setStatusCode(NOT_FOUND.code()).end()
                        } else {
                            response.putHeader("Content-Type", "application/json")
                                    .setStatusCode(OK.code())
                                    .end(Json.encodePrettily(result))
                        }
                    }
                }
    }

    fun deleteMission(context: RoutingContext) {
        val response = context.response()
        val missionId: String? = context.request().getParam("missionId")

        val query = json { obj("_id" to missionId) }
        MongoClient.createNonShared(Main.vertx, MONGODB_CONFIGURATION)
                .removeDocument(MISSIONS_COLLECTION, query) { removeOperation ->
                    when {
                        removeOperation.failed() -> response.setStatusCode(INTERNAL_SERVER_ERROR.code()).end()
                        removeOperation.result().removedCount == 0L -> response.setStatusCode(NOT_FOUND.code()).end()
                        else -> response.setStatusCode(NO_CONTENT.code()).end()
                    }
                }
    }
}
