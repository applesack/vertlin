package xyz.scootaloo.vertlin.dav.router

import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.impl.logging.Logger
import io.vertx.ext.auth.User
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.Json
import io.vertx.kotlin.core.json.obj
import xyz.scootaloo.vertlin.boot.Order
import xyz.scootaloo.vertlin.boot.Ordered
import xyz.scootaloo.vertlin.boot.core.X
import xyz.scootaloo.vertlin.boot.core.currentTimeMillis
import xyz.scootaloo.vertlin.boot.internal.inject
import xyz.scootaloo.vertlin.boot.util.Encoder
import xyz.scootaloo.vertlin.dav.constant.Constant
import xyz.scootaloo.vertlin.dav.constant.ServerDefault
import xyz.scootaloo.vertlin.dav.constant.StatusCode
import xyz.scootaloo.vertlin.dav.service.UserService
import xyz.scootaloo.vertlin.web.HttpRouterRegister
import java.util.*

/**
 * @author flutterdash@qq.com
 * @since 2022/8/4 δΈε5:17
 */
@Order(Ordered.HIGHEST)
class DigestAuthFilter : HttpRouterRegister("/*") {

    private val log = X.getLogger(this::class)
    private val userService by inject(UserService::class)

    override fun register(router: Router) {
        router.any {
            if (it.request().method() == HttpMethod.OPTIONS) {
                it.next()
            } else {
                handle(it)
            }
        }
    }

    private suspend fun handle(ctx: RoutingContext) {
        val headers = ctx.request().headers()
        val authorization = headers[Term.H_AUTHORIZATION] ?: return sendChallenge(ctx)
        val authHeader = Utils.parseAuthHeader(ctx, authorization, log) ?: return sendChallenge(ctx)
        authentication(ctx, authHeader)
    }

    // ι΄ζ
    private suspend fun authentication(ctx: RoutingContext, header: AuthorizationHeader) {
        val (valid, expired) = Utils.validateNonce(header.nonce)
        if (!valid) {
            return sendChallenge(ctx)
        }
        if (expired) {
            return sendChallenge(ctx, true)
        }

        val encodedUsername = Encoder.encode(header.username)
        val password = userService.findPassByName<String>(encodedUsername)
        if (password.isEmpty()) return sendChallenge(ctx)
        val computedResponse = Utils.computedResponse(header, password)
        if (computedResponse == header.response) {
            authorization(ctx, header, password)
        } else {
            sendChallenge(ctx)
        }
    }

    // ζζ
    private fun authorization(ctx: RoutingContext, header: AuthorizationHeader, password: String) {
        val headers = ctx.response().headers()
        headers[Term.H_AUTHENTICATION_INFO] = Utils.authorizationInfo(header, password)

        // δ»ιθΏθ΄¦ε·ε―η ζ₯η‘?ε?η»ιη¨ζ·, δΈθ?°ε½η¨ζ·ηζιδΏ‘ζ―
        // ε¦ζζ, εε°θ·εη¨ζ·ζιηι»θΎεε¨θΏι

        val principal = Json.obj(Constant.USERNAME to header.username)
        ctx.setUser(User.create(principal))

        ctx.next()
    }

    private fun sendChallenge(ctx: RoutingContext, stale: Boolean = false) {
        val response = ctx.response()
        response.putHeader(Term.H_AUTHENTICATE, Utils.buildChallenge(stale))
        response.statusCode = StatusCode.unauthorized
        response.end()
    }

    private class AuthorizationHeader(
        val username: String,
        val realm: String,
        val method: String,
        val uri: String,
        val nonce: String,
        val nonceCounter: String,
        val clientNonce: String,
        val qop: String,
        val response: String
    )

    private object Term {
        const val H_AUTHENTICATE = "WWW-Authenticate" // θ΄¨θ―’
        const val H_AUTHORIZATION = "Authorization"   // εεΊ
        const val H_AUTHENTICATION_INFO = "Authentication-Info"

        const val DIGEST_PREFIX = "Digest"
        const val C_USER = "user"
        const val C_USERNAME = "username"
        const val C_QOP = "qop"
        const val C_RSP_AUTH = "rspauth"
        const val C_CLIENT_NONCE = "cnonce"
        const val C_RESPONSE = "response"
        const val C_NONCE_COUNTER = "nc"
        const val C_NONCE = "nonce"
        const val C_URI = "uri"
        const val C_REALM = "realm"
        const val C_STALE = "stale"

        const val DEF_QOP = "auth"
        const val MAX_NONCE_AGE_SECONDS = 20
    }

    private object Utils {

        fun validateNonce(nonce: String): Pair<Boolean, Boolean> = try {
            val plainNonce = Encoder.base64decode(nonce).trim('\"')
            val timestamp = plainNonce.substring(0, plainNonce.indexOf(' '))
            if (nonce == newNonce(timestamp)) {
                if (currentTimeMillis() - timestamp.toLong() > (Term.MAX_NONCE_AGE_SECONDS * 1000)) {
                    true to true
                } else {
                    true to false
                }
            } else {
                false to false
            }
        } catch (e: Throwable) {
            false to false
        }

        fun buildChallenge(stale: Boolean = false): String {
            val parts = LinkedList<Triple<String, String, Boolean>>()
            parts.add(Triple(Term.C_REALM, ServerDefault.realm, true))
            parts.add(Triple(Term.C_QOP, Term.DEF_QOP, true))
            parts.add(Triple(Term.C_NONCE, newNonce(), true))
            if (stale) {
                parts.add(Triple(Term.C_STALE, "true", false))
            }
            return "${Term.DIGEST_PREFIX} ${format(parts)}"
        }

        fun authorizationInfo(header: AuthorizationHeader, password: String): String {
            return format(
                listOf(
                    Triple(Term.C_QOP, Term.DEF_QOP, true),
                    Triple(Term.C_RSP_AUTH, rspAuth(header, password), true),
                    Triple(Term.C_CLIENT_NONCE, header.clientNonce, true),
                    Triple(Term.C_NONCE_COUNTER, header.nonceCounter, false)
                )
            )
        }

        fun computedResponse(header: AuthorizationHeader, password: String): String {
            val a1hash = header.run { md5("$username:$realm:$password") }
            val a2hash = header.run { md5("$method:$uri") }
            return header.run {
                md5("$a1hash:$nonce:$nonceCounter:$clientNonce:$qop:$a2hash")
            }
        }

        private fun rspAuth(header: AuthorizationHeader, password: String): String {
            val a1Hash = header.run { md5("$username:$realm:$password") }
            val a2Hash = header.run { md5(":$uri") }
            return header.run {
                md5("$a1Hash:$nonce:$nonceCounter:$clientNonce:$qop:$a2Hash")
            }
        }

        fun parseAuthHeader(
            ctx: RoutingContext, authentication: String, log: Logger
        ): AuthorizationHeader? {
            if (authentication.startsWith(Term.DIGEST_PREFIX)) {
                val rest = authentication.substring(Term.DIGEST_PREFIX.length + 1)
                    .replace("\"", "")
                val method = methodOf(ctx.request())
                val result = parseAuthHeaderCore(rest, method)
                if (result.isFailure) {
                    log.warn("ζθ¦ε ε―ζε‘θ­¦ε: δΈθ½θ§£ζηζιδΏ‘ζ―, ζ ΌεΌε―θ½ε­ε¨ιθ―― -> $authentication")
                    return null
                }
                return result.getOrThrow()
            }
            return null
        }

        private fun parseAuthHeaderCore(
            authorization: String, method: String
        ): Result<AuthorizationHeader> {
            val params = HashMap<String, String>()
            for (item in authorization.split(',')) {
                val idx = item.indexOf('=')
                if (idx == -1)
                    continue
                val key = item.substring(0, idx).trim()
                val value = item.substring(idx + 1)
                params[key] = value
            }

            return runCatching {
                AuthorizationHeader(
                    username = params[Term.C_USER]
                        ?: params[Term.C_USERNAME]!!,
                    realm = params[Term.C_REALM]!!,
                    method = method,
                    nonce = params[Term.C_NONCE]!!,
                    uri = params[Term.C_URI]!!,
                    nonceCounter = params[Term.C_NONCE_COUNTER]!!,
                    clientNonce = params[Term.C_CLIENT_NONCE]!!,
                    response = params[Term.C_RESPONSE]!!,
                    qop = params[Term.C_QOP]!!
                )
            }
        }

        private fun newNonce(timestamp: String = currentTimeMillis().toString()): String {
            val secret = md5("$timestamp:${ServerDefault.privateKey}")
            return Encoder.base64encode("\"$timestamp $secret\"")
        }

        private fun format(meta: List<Triple<String, String, Boolean>>): String {
            return meta.joinToString(",") {
                if (it.third) {
                    "${it.first}=\"${it.second}\""
                } else {
                    "${it.first}=${it.second}"
                }
            }
        }

        private fun md5(content: String): String {
            return Encoder.md5(content)
        }

        private fun methodOf(request: HttpServerRequest): String {
            return request.method().toString()
        }

    }

}
