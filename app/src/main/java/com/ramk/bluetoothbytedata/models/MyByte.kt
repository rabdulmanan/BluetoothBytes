package com.ramk.bluetoothbytedata.models

import org.json.JSONObject

data class MyByte(
    var bit1 : Boolean = false,
    var bit2 : Boolean = false,
    var bit3 : Boolean = false,
    var bit4 : Boolean = false,
    var bit5 : Boolean = false,
    var bit6 : Boolean = false,
    var bit7 : Boolean = false,
    var bit8 : Boolean = false,
    var hasFocus : Boolean = false,
    var focusedBit : Int = 0

){

    fun getBytes():Byte{
        /*var byteValue = 0.toByte()

        byteValue = (byteValue.toInt() or (if (bit1) 1 else 0)).toByte()
        byteValue = (byteValue.toInt() or (if (bit2) 1 else 0) shl 1).toByte()
        byteValue = (byteValue.toInt() or (if (bit3) 1 else 0) shl 2).toByte()
        byteValue = (byteValue.toInt() or (if (bit4) 1 else 0) shl 3).toByte()
        byteValue = (byteValue.toInt() or (if (bit5) 1 else 0) shl 4).toByte()
        byteValue = (byteValue.toInt() or (if (bit6) 1 else 0) shl 5).toByte()
        byteValue = (byteValue.toInt() or (if (bit7) 1 else 0) shl 6).toByte()
        byteValue = (byteValue.toInt() or (if (bit8) 1 else 0) shl 7).toByte()*/

        val stringByte = getDisplayBytes()
        val intValue= Integer.parseInt(stringByte,2)
        val byteValue = intValue.toByte()

        val st = String.format("%02X", byteValue)

        return byteValue
    }

    fun getDisplayBytes(): String{

        var byte = (if (bit1) 1 else 0).toString()
        byte += (if (bit2) 1 else 0).toString()
        byte += (if (bit3) 1 else 0).toString()
        byte += (if (bit4) 1 else 0).toString()
        byte += (if (bit5) 1 else 0).toString()
        byte += (if (bit6) 1 else 0).toString()
        byte += (if (bit7) 1 else 0).toString()
        byte += (if (bit8) 1 else 0).toString()

        return byte

    }

    fun toJSON():JSONObject{

        val jsonObject = JSONObject()
        jsonObject.put("bit1",bit1)
        jsonObject.put("bit2",bit2)
        jsonObject.put("bit3",bit3)
        jsonObject.put("bit4",bit4)
        jsonObject.put("bit5",bit5)
        jsonObject.put("bit6",bit6)
        jsonObject.put("bit7",bit7)
        jsonObject.put("bit8",bit8)


        return jsonObject

    }

    fun fromJSONToObject(jsonObject: JSONObject){

        if(jsonObject.has("bit1")){
            bit1 = jsonObject.getBoolean("bit1")
        }
        if(jsonObject.has("bit2")){
            bit2 = jsonObject.getBoolean("bit2")
        }
        if(jsonObject.has("bit3")){
            bit3 = jsonObject.getBoolean("bit3")
        }
        if(jsonObject.has("bit4")){
            bit4 = jsonObject.getBoolean("bit4")
        }
        if(jsonObject.has("bit5")){
            bit5 = jsonObject.getBoolean("bit5")
        }
        if(jsonObject.has("bit6")){
            bit6 = jsonObject.getBoolean("bit6")
        }
        if(jsonObject.has("bit7")){
            bit7 = jsonObject.getBoolean("bit7")
        }
        if(jsonObject.has("bit8")){
            bit8 = jsonObject.getBoolean("bit8")
        }

    }

}
