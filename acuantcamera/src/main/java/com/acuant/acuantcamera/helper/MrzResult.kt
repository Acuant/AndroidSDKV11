package com.acuant.acuantcamera.helper

import java.io.Serializable

data class MrzResult(
    var surName:String = "",
    var givenName:String= "",
    var country:String= "",
    var passportNumber: String= "",
    var nationality:String= "",
    var dob: String= "",
    var gender: String= "",
    var passportExpiration: String= "",
    var personalDocNumber: String= "",
    internal var optionalField1: String= "",
    var checkSumResult1: Boolean = false,
    var checkSumResult2: Boolean = false,
    var checkSumResult3: Boolean = false,
    var checkSumResult4: Boolean = false,
    var checkSumResult5: Boolean = false,
    var threeLineMrz: Boolean = false,
    internal var checkChar1: Char = '<',
    internal var checkChar2: Char = '<',
    internal var checkChar3: Char = '<',
    internal var checkChar4: Char = '<',
    internal var checkChar5: Char = '<'
): Serializable {
    val allCheckSumsPassed: Boolean
        get() = checkSumResult1 && checkSumResult2 && checkSumResult3 && checkSumResult4 && checkSumResult5

    override fun toString(): String {
        return "$country $surName $givenName\n$passportNumber $nationality $dob $gender " +
                "$passportExpiration $personalDocNumber\n" +
                "$checkChar1 $checkSumResult1, $checkChar2 $checkSumResult2, " +
                "$checkChar3 $checkSumResult3, $checkChar4 $checkSumResult4, " +
                "$checkChar5 $checkSumResult5"
    }

    internal fun cleanFields(char: Char = '<') {
        surName.trim().trim(char)
        givenName.trim().trim(char)
        country.trim().trim(char)
        passportNumber.trim().trim(char)
        nationality.trim().trim(char)
        gender.trim().trim(char)
        personalDocNumber.trim().trim(char)
    }
}
