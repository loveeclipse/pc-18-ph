package service

import io.vertx.core.AbstractVerticle
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler

class RouterVerticle : AbstractVerticle() {

    val log = LoggerFactory.getLogger("RouterVerticle")

    override fun start() {
        vertx
                .createHttpServer()
                .requestHandler(createRoute())
                .listen(PORT, HOST)
        log.info("Service ready on port $PORT and host $HOST")
    }

    private fun createRoute(): Router {
        VtService.initializeRequestManager(vertx)
        return Router.router(vertx).apply {
            route().handler(BodyHandler.create())

            get(EVENT_TRACKING_PATH).handler { VtService.retrieveEventTracking(it) }
            get(MISSION_TRACKING_PATH).handler { VtService.retrieveMissionTracking(it) }

            for (item in timelineItems) {
                val path = MISSION_TRACKING_PATH + "/" + item.pathName
                get(path).handler { VtService.retrieveSingleTrackingItem(it, item) }
                put(path).handler { VtService.updateSingleTrackingItem(it, item) }
            }
        }
    }

    companion object {
        private const val PORT = 10000
        private const val HOST = "localhost"

        private const val EVENT_TRACKING_PATH = "/v1/events-tracking/:eventId"
        private const val MISSION_TRACKING_PATH = "$EVENT_TRACKING_PATH/missions/:missionId"

        private val timelineItems = listOf(
                TimelineItem(pathName = "oc-call", fieldName = "ocCall"),
                TimelineItem(pathName = "crew-departure", fieldName = "crewDeparture"),
                TimelineItem(pathName = "arrival-onsite", fieldName = "arrivalOnsite"),
                TimelineItem(pathName = "departure-onsite", fieldName = "departureOnsite"),
                TimelineItem(pathName = "landing-helipad", fieldName = "landingHelipad"),
                TimelineItem(pathName = "arrival-er", fieldName = "arrivalEr")
        )

        private const val CHOSEN_HOSPITAL_PATH = ""
    }
}