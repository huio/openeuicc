package im.angry.openeuicc.core

import im.angry.openeuicc.util.*
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.LocalProfileAssistant

interface EuiccChannel {
    val type: String

    val port: UiccPortInfoCompat

    val slotId: Int // PHYSICAL slot
    val logicalSlotId: Int
    val portId: Int

    /**
     * Some chips support multiple SEs on one chip. The seId here is intended
     * to distinguish channels opened from these different SEs.
     *
     * Note that this ID is arbitrary and heavily depends on the order in which
     * we attempt to open the ISD-R AIDs. As such, it shall not be treated with
     * any significance other than as a transient ID.
     */
    val seId: Int

    val lpa: LocalProfileAssistant

    val valid: Boolean

    /**
     * Answer to Reset (ATR) value of the underlying interface, if any
     */
    val atr: ByteArray?

    /**
     * Intrinsic name of this channel. For device-internal SIM slots,
     * this should be null; for USB readers, this should be the name of
     * the reader device.
     */
    val intrinsicChannelName: String?

    /**
     * The underlying APDU interface for this channel
     */
    val apduInterface: ApduInterface

    /**
     * The AID of the ISD-R channel currently in use
     */
    val isdrAid: ByteArray

    fun close()
}