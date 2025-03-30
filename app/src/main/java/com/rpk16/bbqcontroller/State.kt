package com.rpk16.bbqcontroller

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable

@Serializable
data class State(
    val fanSpeed: Int,
    val probePitTemp: Int,
    val probe1Temp: Int,
    val probe2Temp: Int,
    val probe3Temp: Int,
    val targetPitTemp: Int,
    val targetFoodTemp: Int,
    val activeCookSession: Int,
    val timestamp: Int,
    var cookSessionGuid: String?
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readInt(),                      // fanSpeed
        parcel.readInt(),                      // probePitTemp
        parcel.readInt(),                      // probe1Temp
        parcel.readInt(),                      // probe2Temp
        parcel.readInt(),                      // probe3Temp
        parcel.readInt(),                      // targetPitTemp
        parcel.readInt(),                      // targetFoodTemp
        parcel.readInt(),                      // activeCookSession
        parcel.readInt(),                      // timestamp
        parcel.readString()                    // cookSessionGuid
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(fanSpeed)
        parcel.writeInt(probePitTemp)
        parcel.writeInt(probe1Temp)
        parcel.writeInt(probe2Temp)
        parcel.writeInt(probe3Temp)
        parcel.writeInt(targetPitTemp)
        parcel.writeInt(targetFoodTemp)
        parcel.writeInt(activeCookSession)
        parcel.writeInt(timestamp)

        parcel.writeString(cookSessionGuid)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<State> {
        override fun createFromParcel(parcel: Parcel): State {
            return State(parcel)
        }

        override fun newArray(size: Int): Array<State?> {
            return arrayOfNulls(size)
        }
    }
}
