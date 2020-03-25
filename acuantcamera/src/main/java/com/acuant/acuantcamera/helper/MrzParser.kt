package com.acuant.acuantcamera.helper

import android.util.Log

class MrzParser{

    companion object {
        private const val FILLER = '<'
        private const val PASSPORT_FIRST_VALUE = 'P'
        private const val CHECK_SUM_ARRAY = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    }

    fun parseMrz(mrz:String): MrzResult?{
        val mrzWithNoSpaces = mrz.replace(" ",  "")
        val mrzLinesTmp = mrzWithNoSpaces.split('\n')
        val mrzLines: MutableList<String> = mutableListOf()
        for (i in mrzLinesTmp.indices) {
            if(mrzLinesTmp[i].length > 6) {
                mrzLines.add(mrzLinesTmp[i])
            }
        }
        if(mrzLines.size == 2){
            val result = parseFirstLine(mrzLines[0])
            //not checking result of first line as it is not strictly needed for echip reading
            return parseSecondLine(mrzLines[1], result)
        }
        return null
    }

    private fun parseFirstLine(firstLine:String): MrzResult?{
        var startPos = 0
        if(firstLine[startPos] == PASSPORT_FIRST_VALUE && firstLine.length == 44){
            startPos+=2

            val country = firstLine.substring(startPos, startPos+3)
            startPos+=3

            var nextPos = firstLine.indexOf("$FILLER$FILLER", startPos)
            val surname = firstLine.substring(startPos, nextPos)
            startPos = nextPos+2

            nextPos = firstLine.indexOf("$FILLER$FILLER", startPos)
            val givenName = firstLine.substring(startPos, nextPos)
            return MrzResult(surName = surname, givenName = givenName, country = country)
        }
        return null
    }

    private fun parseSecondLine(line:String, result: MrzResult?): MrzResult?{

        //Log.d("SecondLine:", line)

        if(line.length != 44){
            return null
        }

        var resultLocal = result

        if(resultLocal == null) {
            resultLocal = MrzResult()
        }

        var startPos = 0
        resultLocal.passportNumber = line.substring(startPos, startPos+9)
        startPos+=9

        var check1 = line[startPos]
        resultLocal.checkSumResult1 = checkSum(resultLocal.passportNumber, check1)
        ++startPos

        if (!resultLocal.checkSumResult1) {
            resultLocal.passportNumber = resultLocal.passportNumber.replace('O', '0')
            resultLocal.checkSumResult1 = checkSum(resultLocal.passportNumber, check1)

            if (!resultLocal.checkSumResult1 && check1 == 'O') {
                check1 = '0'
                resultLocal.checkSumResult1 = checkSum(resultLocal.passportNumber, check1)
            }
        }

        resultLocal.nationality = line.substring(startPos, startPos+3)
        startPos+=3

        resultLocal.dob = line.substring(startPos, startPos+6)
        startPos+=6

        var check2 = line[startPos]
        resultLocal.checkSumResult2 = checkSum(resultLocal.dob, check2)
        ++startPos

        if (!resultLocal.checkSumResult2) {
            resultLocal.dob = resultLocal.dob.replace('O', '0')
            resultLocal.checkSumResult2 = checkSum(resultLocal.dob, check2)

            if (!resultLocal.checkSumResult2 && check2 == 'O') {
                check2 = '0'
                resultLocal.checkSumResult2 = checkSum(resultLocal.dob, check2)
            }
        }

        resultLocal.gender = line.substring(startPos, startPos+1)
        ++startPos

        resultLocal.passportExpiration = line.substring(startPos, startPos+6)
        startPos+=6

        var check3 = line[startPos]
        resultLocal.checkSumResult3 = checkSum(resultLocal.passportExpiration, check3)
        ++startPos

        if (!resultLocal.checkSumResult3) {
            resultLocal.passportExpiration = resultLocal.passportExpiration.replace('O', '0')
            resultLocal.checkSumResult3 = checkSum(resultLocal.passportExpiration, check3)

            if (!resultLocal.checkSumResult3 && check3 == 'O') {
                check3 = '0'
                resultLocal.checkSumResult3 = checkSum(resultLocal.passportExpiration, check3)
            }
        }

        resultLocal.personalDocNumber = line.substring(startPos, startPos+14)
        startPos+=14

        var check4 = line[startPos]
        resultLocal.checkSumResult4 = checkSum(resultLocal.personalDocNumber, check4)
        ++startPos

        if (!resultLocal.checkSumResult4) {
            resultLocal.personalDocNumber = resultLocal.personalDocNumber.replace('O', '0')
            resultLocal.checkSumResult4 = checkSum(resultLocal.personalDocNumber, check4)

            if (!resultLocal.checkSumResult4 && check4 == 'O') {
                check4 = '0'
                resultLocal.checkSumResult4 = checkSum(resultLocal.personalDocNumber, check4)
            }
        }

        val finalCheckString = resultLocal.passportNumber + check1 + resultLocal.dob + check2 +
                resultLocal.passportExpiration + check3 + resultLocal.personalDocNumber + check4
        resultLocal.checkSumResult5 = checkSum(finalCheckString, line[startPos])

        if (!resultLocal.checkSumResult5 && line[startPos] == 'O') {
            resultLocal.checkSumResult5 = checkSum(resultLocal.personalDocNumber, '0')
        }
        Log.d("SecondLine:", "" + resultLocal.checkSumResult1 + " " + resultLocal.checkSumResult2 + " " + resultLocal.checkSumResult3 + " " + resultLocal.checkSumResult4 + " " + resultLocal.checkSumResult5)

        return resultLocal
    }

    private fun checkSum(input: String, checkSumChar:Char): Boolean{
        var count = 0
        val checkSumValue = getValue(checkSumChar)
        for(i in input.indices){
            count += getValue(input[i]) * getWeight(i)
        }
        return checkSumValue == count % 10
    }

    /**
     * The weight that each value is multiplied by.
     * The weight of the first position is 7, of the second it is 3, and of the third it is 1,
     * after that the weights repeat 7, 3, 1, and so on.
     */
    private fun getWeight(position: Int): Int{
        return when {
            position%3 == 0 -> 7
            position%3 == 1 -> 3
            else -> 1
        }
    }

    /**
     * Each position is assigned a value; for the digits 0 to 9 this is the value of the digits,
     * for the letters A to Z this is 10 to 35, for the filler < this is 0.
     */
    private fun getValue(character: Char): Int{
        val value = CHECK_SUM_ARRAY.indexOf(character)
        return if (value > 0) value else 0
    }
}