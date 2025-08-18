package im.angry.openeuicc.ui.wizard

import androidx.annotation.StringRes
import im.angry.openeuicc.common.R
import net.typeblog.lpac_jni.LocalProfileAssistant
import org.json.JSONObject
import java.net.NoRouteToHostException
import java.net.PortUnreachableException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

object SimplifiedErrorHandling {
    enum class ErrorCode(@StringRes val titleResId: Int, @StringRes val suggestResId: Int?) {
        ICCIDAlready(
            R.string.download_wizard_error_iccid_already,
            R.string.download_wizard_error_suggest_profile_installed
        ),
        InsufficientMemory(
            R.string.download_wizard_error_insufficient_memory,
            R.string.download_wizard_error_suggest_insufficient_memory
        ),
        UnsupportedProfile(
            R.string.download_wizard_error_unsupported_profile,
            null
        ),
        CardInternalError(
            R.string.download_wizard_error_card_internal_error,
            null
        ),
        EIDMismatch(
            R.string.download_wizard_error_eid_mismatch,
            R.string.download_wizard_error_suggest_contact_reissue
        ),
        UnreleasedProfile(
            R.string.download_wizard_error_profile_unreleased,
            R.string.download_wizard_error_suggest_contact_reissue
        ),
        MatchingIDRefused(
            R.string.download_wizard_error_matching_id_refused,
            R.string.download_wizard_error_suggest_contact_carrier
        ),
        ProfileRetriesExceeded(
            R.string.download_wizard_error_profile_retries_exceeded,
            R.string.download_wizard_error_suggest_contact_carrier
        ),
        ConfirmationCodeMissing(
            R.string.download_wizard_error_confirmation_code_missing,
            R.string.download_wizard_error_suggest_contact_carrier
        ),
        ConfirmationCodeRefused(
            R.string.download_wizard_error_confirmation_code_refused,
            R.string.download_wizard_error_suggest_contact_carrier
        ),
        ConfirmationCodeRetriesExceeded(
            R.string.download_wizard_error_confirmation_code_retries_exceeded,
            R.string.download_wizard_error_suggest_contact_carrier
        ),
        ProfileExpired(
            R.string.download_wizard_error_profile_expired,
            R.string.download_wizard_error_suggest_contact_carrier
        ),
        UnknownHost(
            R.string.download_wizard_error_unknown_hostname,
            null
        ),
        NetworkUnreachable(
            R.string.download_wizard_error_network_unreachable,
            R.string.download_wizard_error_suggest_network_unreachable
        ),
        TLSError(
            R.string.download_wizard_error_tls_certificate,
            null
        )
    }

    private val httpErrors = buildMap {
        // Stage: AuthenticateClient
        put("8.1" to "4.8", ErrorCode.InsufficientMemory)
        put("8.1.1" to "3.8", ErrorCode.EIDMismatch)
        put("8.2" to "1.2", ErrorCode.UnreleasedProfile)
        put("8.2.6" to "3.8", ErrorCode.MatchingIDRefused)
        put("8.8.5" to "6.4", ErrorCode.ProfileRetriesExceeded)

        // Stage: GetBoundProfilePackage
        put("8.2.7" to "2.2", ErrorCode.ConfirmationCodeMissing)
        put("8.2.7" to "3.8", ErrorCode.ConfirmationCodeRefused)
        put("8.2.7" to "6.4", ErrorCode.ConfirmationCodeRetriesExceeded)

        // Stage: AuthenticateClient, GetBoundProfilePackage
        put("8.8.5" to "4.10", ErrorCode.ProfileExpired)
    }

    fun toSimplifiedDownloadError(exc: LocalProfileAssistant.ProfileDownloadException) = when {
        exc.lpaErrorReason != "ES10B_ERROR_REASON_UNDEFINED" -> toSimplifiedLPAErrorReason(exc.lpaErrorReason)
        exc.lastHttpResponse?.rcode == 200 -> toSimplifiedHTTPResponse(exc.lastHttpResponse!!)
        exc.lastHttpException != null -> toSimplifiedHTTPException(exc.lastHttpException!!)
        exc.lastApduResponse != null -> toSimplifiedAPDUResponse(exc.lastApduResponse!!)
        else -> null
    }

    private fun toSimplifiedLPAErrorReason(reason: String) = when (reason) {
        "ES10B_ERROR_REASON_UNSUPPORTED_CRT_VALUES" -> ErrorCode.UnsupportedProfile
        "ES10B_ERROR_REASON_UNSUPPORTED_REMOTE_OPERATION_TYPE" -> ErrorCode.UnsupportedProfile
        "ES10B_ERROR_REASON_UNSUPPORTED_PROFILE_CLASS" -> ErrorCode.UnsupportedProfile
        "ES10B_ERROR_REASON_INSTALL_FAILED_DUE_TO_ICCID_ALREADY_EXISTS_ON_EUICC" -> ErrorCode.ICCIDAlready
        "ES10B_ERROR_REASON_INSTALL_FAILED_DUE_TO_INSUFFICIENT_MEMORY_FOR_PROFILE" -> ErrorCode.InsufficientMemory
        "ES10B_ERROR_REASON_INSTALL_FAILED_DUE_TO_INTERRUPTION" -> ErrorCode.CardInternalError
        "ES10B_ERROR_REASON_INSTALL_FAILED_DUE_TO_PE_PROCESSING_ERROR" -> ErrorCode.CardInternalError
        else -> null
    }

    private fun toSimplifiedHTTPResponse(response: net.typeblog.lpac_jni.HttpInterface.HttpResponse): ErrorCode? {
        if (response.data.first().toInt() != '{'.code) return null
        val response = JSONObject(response.data.decodeToString())
        val statusCodeData = response.optJSONObject("header")
            ?.optJSONObject("functionExecutionStatus")
            ?.optJSONObject("statusCodeData")
            ?: return null
        val subjectCode = statusCodeData.optString("subjectCode")
        val reasonCode = statusCodeData.optString("reasonCode")
        return httpErrors[subjectCode to reasonCode]
    }

    private fun toSimplifiedHTTPException(exc: Exception) = when (exc) {
        is SSLException -> ErrorCode.TLSError
        is UnknownHostException -> ErrorCode.UnknownHost
        is NoRouteToHostException -> ErrorCode.NetworkUnreachable
        is PortUnreachableException -> ErrorCode.NetworkUnreachable
        is SocketTimeoutException -> ErrorCode.NetworkUnreachable
        is SocketException -> exc.message
            ?.contains("Connection reset", ignoreCase = true)
            ?.let { if (it) ErrorCode.NetworkUnreachable else null }
        else -> null
    }

    private fun toSimplifiedAPDUResponse(resp: ByteArray): ErrorCode? {
        val isSuccess = resp.size >= 2 &&
                resp[resp.size - 2] == 0x90.toByte() &&
                resp[resp.size - 1] == 0x00.toByte()
        if (isSuccess) return null
        return ErrorCode.CardInternalError
    }
}
