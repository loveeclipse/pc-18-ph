package service

import io.vertx.core.Vertx
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

object ManouversService {
    var vertx: Vertx? = null
    private val MONGODB_CONFIGURATION = json { obj(
            "connection_string" to "mongodb://loveeclipse:PC-preh2019@ds149676.mlab.com:49676/heroku_jw7pjmcr"
    ) }
}