package services

import io.netty.handler.codec.http.HttpResponseStatus.CREATED
import io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST
import io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR
import io.vertx.core.Vertx
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import java.util.UUID

import utils.MongoUtils.checkSchema
import utils.MongoUtils.isDuplicateKey

object InjectionTreatmentsService {

    private val log = LoggerFactory.getLogger(this.javaClass.simpleName)

    private const val COLLECTION_NAME = "injectiontreatments"
    private const val PATIENT_ID = "patientId"
    private const val DOCUMENT_ID = "_id"
    private val INJECTION_TREATMENT_SCHEMA = listOf("name", "caliber", "time")

    var vertx: Vertx? = null
    private val MONGODB_CONFIGURATION = json { obj(
            "connection_string" to "mongodb://loveeclipse:PC-preh2019@ds149676.mlab.com:49676/heroku_jw7pjmcr"
    ) }

    fun createInjectionTreatment(routingContext: RoutingContext) {
        log.info("Request to create a injection treatment")
        val response = routingContext.response()
        val injectionTreatmentData = routingContext.bodyAsJson
        val patientId = routingContext.request().params()[PATIENT_ID]
        val injectionTreatmentId = UUID.randomUUID().toString()
        val uri = routingContext.request().absoluteURI().plus("/$injectionTreatmentId")
        if (checkSchema(injectionTreatmentData, INJECTION_TREATMENT_SCHEMA, INJECTION_TREATMENT_SCHEMA)) {
            val document = injectionTreatmentData
                    .put(DOCUMENT_ID, injectionTreatmentId)
                    .put(PATIENT_ID, patientId)
            MongoClient.createNonShared(vertx, MONGODB_CONFIGURATION)
                    .insert(COLLECTION_NAME, document) { insertOperation ->
                        when {
                            insertOperation.succeeded() ->
                                response
                                        .putHeader("Content-Type", "text/plain")
                                        .putHeader("Location", uri)
                                        .setStatusCode(CREATED.code())
                                        .end(injectionTreatmentId)
                            isDuplicateKey(insertOperation.cause().message) ->
                                createInjectionTreatment(routingContext)
                            else ->
                                response.setStatusCode(INTERNAL_SERVER_ERROR.code()).end()
                        }
                    }
        } else response.setStatusCode(BAD_REQUEST.code()).end()
    }
}