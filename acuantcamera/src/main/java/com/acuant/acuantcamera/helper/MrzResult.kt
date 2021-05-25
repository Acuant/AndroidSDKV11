package com.acuant.acuantcamera.helper

import java.io.Serializable

data class MrzResult(var surName:String = "",
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
                     internal var checkChar1: Char = '<'): Serializable
