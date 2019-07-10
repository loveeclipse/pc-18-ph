package services

import io.netty.handler.codec.http.HttpResponseStatus.CREATED
import io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST
import io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR
import io.vertx.core.Vertx
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.web.RoutingContext
import java.util.UUID

import utils.MongoUtils.checkSchema
import utils.MongoUtils.isDuplicateKey
import utils.MongoUtils.MONGODB_CONFIGURATION

object SimpleTreatmentsService {

    private val log = LoggerFactory.getLogger(this.javaClass.simpleName)

    private const val COLLECTION_NAME = "simpletreatments"
    private const val PATIENT_ID = "patientId"
    private const val DOCUMENT_ID = "_id"
    private val SIMPLE_TREATMENT_SCHEMA = listOf("name", "time")

    var vertx: Vertx? = null

    fun createSimpleTreatment(routingContext: RoutingContext) {
        log.info("Request to create a simple treatment")
        val response = routingContext.response()
        val simpleTreatmentData = routingContext.bodyAsJson
        val patientId = routingContext.request().params()[PATIENT_ID]
        val simpleTreatmentId = UUID.randomUUID().toString()
        val uri = routingContext.request().absoluteURI().plus("/$simpleTreatmentId")
        if (checkSchema(simpleTreatmentData, SIMPLE_TREATMENT_SCHEMA, SIMPLE_TREATMENT_SCHEMA)) {
            val document = simpleTreatmentData
                    .put(DOCUMENT_ID, simpleTreatmentId)
                    .put(PATIENT_ID, patientId)
            MongoClient.createNonShared(vertx, MONGODB_CONFIGURATION)
                    .insert(COLLECTION_NAME, document) { insertOperation ->
                        when {
                            insertOperation.succeeded() ->
                                response
                                        .putHeader("Content-Type", "text/plain")
                                        .putHeader("Location", uri)
                                        .setStatusCode(CREATED.code())
                                        .end(simpleTreatmentId)
                            isDuplicateKey(insertOperation.cause().message) ->
                                createSimpleTreatment(routingContext)
                            else ->
                                response.setStatusCode(INTERNAL_SERVER_ERROR.code()).end()
                        }
                    }
        } else response.setStatusCode(BAD_REQUEST.code()).end()
    }
}