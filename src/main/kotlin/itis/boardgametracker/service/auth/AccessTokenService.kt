package itis.boardgametracker.service.auth

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import itis.boardgametracker.exception.InvalidCredentialsException
import itis.boardgametracker.model.User
import itis.boardgametracker.properties.JwtProperties
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class AccessTokenService(
    private val objectMapper: ObjectMapper,
    private val jwtProperties: JwtProperties
) {
    data class AccessTokenClaims(
        val userId: Long,
        val email: String,
        val roles: List<String>
    )

    fun generate(user: User): String {
        val now = Instant.now()
        val headerJson = objectMapper.writeValueAsString(
            mapOf(
                "alg" to "HS256",
                "typ" to "JWT"
            )
        )
        val payloadJson = objectMapper.writeValueAsString(
            mapOf(
                "iss" to jwtProperties.issuer,
                "sub" to user.id.toString(),
                "iat" to now.epochSecond,
                "exp" to now.plusSeconds(jwtProperties.accessTokenTtlSeconds).epochSecond,
                "userId" to user.id,
                "email" to user.email,
                "roles" to user.roles.map { "ROLE_${it.name}" }
            )
        )
        val header = encode(headerJson.toByteArray(StandardCharsets.UTF_8))
        val payload = encode(payloadJson.toByteArray(StandardCharsets.UTF_8))
        val signature = sign("$header.$payload")
        return "$header.$payload.$signature"
    }


    fun extract(token: String): AccessTokenClaims {
        val payload = parseAndValidate(token)
        val userId = (payload["userId"] as Number).toLong()
        val email = payload["email"] as String
        val roles = (payload["roles"] as List<*>).map { it.toString() }
        return AccessTokenClaims(
            userId = userId,
            email = email,
            roles = roles
        )
    }

    fun expiresInSeconds(): Int {
        return jwtProperties.accessTokenTtlSeconds.toInt()
    }

    private fun parseAndValidate(token: String): Map<String, Any> {
        val parts = token.split(".")
        if (parts.size != 3) {
            throw InvalidCredentialsException()
        }
        val payloadBytes = decode(parts[1])
        val payload = objectMapper.readValue(payloadBytes, object : TypeReference<Map<String, Any>>() {})

        val expectedSignature = sign("${parts[0]}.${parts[1]}")
        if (parts[2] != expectedSignature) {
            throw InvalidCredentialsException()
        }
        if (payload["iss"] != jwtProperties.issuer) {
            throw InvalidCredentialsException()
        }
        val exp = (payload["exp"] as Number).toLong()
        if (Instant.now().epochSecond >= exp) {
            throw InvalidCredentialsException()
        }
        return payload
    }

    private fun sign(content: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val key = SecretKeySpec(jwtProperties.secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        mac.init(key)
        val signed = mac.doFinal(content.toByteArray(StandardCharsets.UTF_8))
        return encode(signed)
    }

    private fun encode(bytes: ByteArray): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun decode(value: String): ByteArray {
        return Base64.getUrlDecoder().decode(value)
    }
}
