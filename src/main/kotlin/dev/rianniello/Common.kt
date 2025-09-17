package dev.rianniello

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.net.URLDecoder
import java.time.Instant
import java.util.*
import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT

val client = HttpClient(CIO) { install(ContentNegotiation) { jackson() } }

fun arg(name: String, args: Array<String>, default: String? = null): String =
    args.indexOf(name).takeIf { it >= 0 }?.let { idx -> args.getOrNull(idx + 1) }
        ?: default ?: error("Missing $name")

fun urlParam(name: String, uri: String): String? =
    Regex("""[?&]$name=([^&]+)""").find(uri)?.groupValues?.get(1)?.let { URLDecoder.decode(it, "UTF-8") }

fun saveText(path: String, content: String) {
    File(path).writeText(content)
    println("Saved: $path")
}

// Build a simple HS256 vp_token for demo (verifier isn’t checking VP signature strictly in your PoC)
fun buildVpJwt(vc: String, nonce: String, aud: String): String {
    val now = Instant.now()
    val claims = JWTClaimsSet.Builder()
        .issuer("did:example:holder")
        .audience(aud)
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
    return SignedJWT(header, claims).apply {
        sign(MACSigner("01234567890123456789012345678901")) // 32-byte demo secret
    }.serialize()
}
