package dev.rianniello

import io.ktor.client.request.*
import io.ktor.client.call.*

suspend fun main(args: Array<String>) {
    val verifier = arg("--verifier", args, "http://localhost:9080")
    val vcPath = arg("--vc", args, "wallet-vc.jwt")

    // Option A: fetch an auth request from the verifier
    data class AR(val authorization_request_uri: String, val state: String, val nonce: String)
    val ar: AR = client.get("$verifier/authorize-presentation").body()

    val arUri = ar.authorization_request_uri
    val state = ar.state
    val nonce = ar.nonce
    println("AR: state=$state nonce=$nonce")
    println("URI: $arUri")

    val vc = java.io.File(vcPath).readText().trim()
    val vpToken = buildVpJwt(vc, nonce = nonce, aud = "$verifier/callback")

    val result: String = client.get("$verifier/callback") {
        parameter("state", state)
        parameter("vp_token", vpToken)
        parameter("presentation_submission", "{}")
    }.body()

    println("Verifier response: $result")
}
