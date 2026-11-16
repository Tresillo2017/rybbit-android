package app.rybbitclient.testsupport

import app.rybbitclient.core.network.Credential
import app.rybbitclient.core.network.HttpClientFactory
import app.rybbitclient.core.network.RybbitApi
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import java.io.File

/** Loads a file from docs/fixtures. Gradle runs unit tests from `app/`, so `../docs` is tried first. */
object FixtureFiles {
    private val candidates = listOf("../docs/fixtures", "docs/fixtures")

    fun read(name: String): String {
        val file = candidates.map { File(it, name) }.firstOrNull { it.exists() }
            ?: error("Fixture $name not found under ${candidates.joinToString()}")
        return file.readText()
    }
}

/** Builds a [RybbitApi] whose engine answers every request with [body]. Requests are appended to [captured]. */
fun apiReplying(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    retryAfter: String? = null,
    captured: MutableList<io.ktor.client.request.HttpRequestData> = mutableListOf(),
    credential: Credential? = Credential.ApiKey("test-key"),
): RybbitApi {
    val engine = MockEngine { request ->
        captured += request
        respondJson(body, status, retryAfter)
    }
    val client = HttpClientFactory(USER_AGENT, engine).create(credential)
    return RybbitApi(client, BASE_URL)
}

fun MockRequestHandleScope.respondJson(
    body: String,
    status: HttpStatusCode,
    retryAfter: String? = null,
): HttpResponseData {
    val headers = HeadersBuilder().apply {
        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        if (retryAfter != null) append(HttpHeaders.RetryAfter, retryAfter)
    }.build()
    return respond(content = body, status = status, headers = headers)
}

/** Response headers containing one `Set-Cookie` per value. */
fun cookieHeaders(vararg cookies: String): Headers {
    val builder = HeadersBuilder()
    builder.append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    cookies.forEach { builder.append(HttpHeaders.SetCookie, it) }
    return builder.build()
}

const val BASE_URL = "https://analytics.example.test"
const val USER_AGENT = "RybbitAndroid/0.1.0-test"
