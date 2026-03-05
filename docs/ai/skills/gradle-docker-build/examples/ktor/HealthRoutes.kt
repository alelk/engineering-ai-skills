package com.example.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

/**
 * Health check response model.
 *
 * Follows common conventions for container orchestrators:
 * - Docker HEALTHCHECK expects HTTP 200.
 * - Kubernetes uses `/health/live` (liveness) and `/health/ready` (readiness).
 */
@Serializable
data class HealthResponse(
    val status: String = "ok",
    val service: String = "my-server", // TODO: change to your service name
)

/**
 * Health check routes.
 *
 * | Endpoint          | Purpose                                    |
 * |-------------------|--------------------------------------------|
 * | `GET /health`     | Basic health — always returns 200 if alive |
 * | `GET /health/ready` | Readiness — can include DB/cache checks  |
 * | `GET /health/live`  | Liveness — lightweight, no dependency checks |
 */
fun Application.healthRoutes() {
    routing {
        /**
         * Basic health check.
         *
         * Used by Docker HEALTHCHECK and simple load-balancer probes.
         *
         * @response 200 [HealthResponse] Service is healthy.
         */
        get("/health") {
            call.respond(HttpStatusCode.OK, HealthResponse())
        }

        /**
         * Readiness probe.
         *
         * Indicates the service is ready to accept traffic.
         * Add downstream dependency checks here (DB ping, cache, etc.)
         * to prevent traffic routing before the service is fully initialised.
         *
         * @response 200 [HealthResponse] Service is ready.
         * @response 503 Service is not ready.
         */
        get("/health/ready") {
            // TODO: add real readiness checks, e.g.:
            //   val dbOk = dataSource.connection.use { it.isValid(2) }
            //   if (!dbOk) return@get call.respond(HttpStatusCode.ServiceUnavailable, HealthResponse(status = "not_ready"))
            call.respond(HttpStatusCode.OK, HealthResponse(status = "ready"))
        }

        /**
         * Liveness probe.
         *
         * Indicates the service process is alive. Must be lightweight —
         * do NOT check downstream dependencies here. If liveness fails,
         * orchestrators will restart the container.
         *
         * @response 200 [HealthResponse] Service is alive.
         */
        get("/health/live") {
            call.respond(HttpStatusCode.OK, HealthResponse(status = "live"))
        }
    }
}

