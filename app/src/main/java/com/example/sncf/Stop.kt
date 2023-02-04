package com.example.sncf

class Stop(private var hourArrival:String, private var minuteArrival:String, private var hourDeparture:String, private var minuteDeparture:String, private var station:Station) {

    override fun toString(): String {
        return "Arrivée à $hourArrival:$minuteArrival, départ à $hourDeparture:$minuteDeparture à $station"
    }
}