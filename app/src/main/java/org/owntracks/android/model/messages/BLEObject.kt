package org.owntracks.android.model.messages

import com.fasterxml.jackson.annotation.JsonProperty
import org.owntracks.android.model.BatteryStatus
import java.util.*

data class BLEObject(

    @JsonProperty("name") var name: String? = null,

    @JsonProperty("rssi")  var rssi : Int,

    @JsonProperty("uuid") var uuid: String? = null,
    @JsonProperty("date") var date:Date,
    @JsonProperty("address") var address:String
)
