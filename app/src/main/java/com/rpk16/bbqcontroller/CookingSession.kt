package com.rpk16.bbqcontroller

data class CookingSession(
    var id: Int? = null,
    var sessionGuid: String? = null,
    val startTime: Long?,
    var endTime: Long?,
    var targetFoodTemp: Int? = null,
    var targetPitTemp: Int? = null,
    var state: State? = null
)



