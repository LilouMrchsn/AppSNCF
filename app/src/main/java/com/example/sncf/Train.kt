package com.example.sncf

enum class TypeTrain {
    TER,
    INTERCITES,
    TGV,
    OUIGO
}

class Train(var num:Int, var type:TypeTrain,
            var localHour: String, var localMinute:String) {

    lateinit var from:Stop
    lateinit var to:Stop
    var stops:ArrayList<Stop> = ArrayList()

    override fun toString(): String {
        return "Train $num de $type de $from à $to à $localHour:$localMinute"
    }

    fun addStop(stop:Stop, departureStation:Boolean, arrivalStation:Boolean) {
        stops.add(stop)
        if(departureStation) {
            from = stop
        }
        if(arrivalStation) {
            to = stop
        }
    }
}
