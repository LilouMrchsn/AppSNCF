package com.example.sncf

class Station(var CODE_UIC:Int?, var libelle:String, var long:Double?, var lat:Double?) {
    override fun toString(): String {
        return libelle
    }
}