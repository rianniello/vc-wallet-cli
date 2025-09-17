package dev.rianniello

import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.http.*

suspend fun main(args: Array<String>) {
    val issuer = arg("--issuer", args, "http://localhost:8080")
    val code = arg("--code", args, "PREAUTH-123")
    val out = arg("--out", args, "wallet-vc.jwt")

    println("Issuing from $issuer with pre-authorized code $code ...")

    val tokenResp: Map<String, Any?> = client.post("$issuer/oauth2/token") {
        contentType(ContentType.Application.FormUrlEncoded)
        setBody(
            listOf(
                "grant_type" to "urn:ietf:params:oauth:grant-type:pre-authorized_code",
                "pre-authorized_code" to code
            ).formUrlEncode()
        )
    }.body()
    val accessToken = tokenResp["access_token"]?.toString()
        ?: error("No access_token in response: $tokenResp")

    val credResp: Map<String, Any?> = client.post("$issuer/credential") {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.Authorization, "Bearer $accessToken")
        setBody(mapOf(
            "format" to "jwt_vc_json",
            "types" to listOf("VerifiableCredential","UniversityID")
        ))
    }.body()

    val vc = credResp["credential"]?.toString() ?: error("No 'credential' in response: $credResp")
    saveText(out, vc)
    println("Issued VC (first 40 chars): ${vc.take(40)}")
}
