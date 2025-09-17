package dev.rianniello
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import io.ktor.client.request.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.time.Instant
import java.util.*
import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

suspend fun main() {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) { jackson() }
    }
    // === ISSUANCE ===
    println("1) Issuance")
    println("Fetching access token...")
    val tokenResp: Map<String, Any?> = client.post("http://localhost:8080/oauth2/token") {
        header("Content-Type", "application/x-www-form-urlencoded")
        setBody("grant_type=urn:ietf:params:oauth:grant-type:pre-authorized_code&pre-authorized_code=PREAUTH-123")
    }.body()
    val accessToken = tokenResp["access_token"] as String

    println("Requesting credential...")
    val credResp: Map<String, Any?> = client.post("http://localhost:8080/credential") {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.Authorization, "Bearer $accessToken")
        setBody(
            mapOf(
                "format" to "jwt_vc_json",
                "types" to listOf("VerifiableCredential", "UniversityID")
                // include "proof" if your issuer requires it
            )
        )
    }.body()
    val vc = credResp["credential"] as String
    File("wallet-vc.jwt").writeText(vc)
    println("Saved VC to wallet-vc.jwt")

    // VERIFY
    println("2) Presentation")
    data class AR(val authorization_request_uri: String, val state: String, val nonce: String)

    val ar: AR = client.get("http://localhost:9080/authorize-presentation").body()
    val arUri = ar.authorization_request_uri
    val state = ar.state
    val nonce = ar.nonce
    println("Got AR from verifier:\nstate=$state\nnonce=$nonce\nuri=$arUri")

    // Build vp_token (simplified, holder uses HS256 here for demo)
    val now = Instant.now()
    val claims = JWTClaimsSet.Builder()
        .issuer("did:example:holder")
        .audience("http://localhost:9080/callback")
        .claim("nonce", nonce)
        .claim("vp", mapOf(
            "@context" to listOf("https://www.w3.org/2018/credentials/v1"),
            "type" to listOf("VerifiablePresentation"),
            "verifiableCredential" to listOf(vc)
        ))
        .issueTime(Date.from(now))
        .expirationTime(Date.from(now.plusSeconds(600)))
        .jwtID(UUID.randomUUID().toString())
        .build()
    val header = JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build()
    val vp = SignedJWT(header, claims)
    vp.sign(MACSigner("01234567890123456789012345678901"))
    val vpToken = vp.serialize()

    println("Submitting to verifier...")
    val verifyResp: String = client.get("http://localhost:9080/callback") {
        parameter("state", state)
        parameter("vp_token", vpToken)
        parameter("presentation_submission", "{}")
    }.body()
    println("Verifier said: $verifyResp")
}
