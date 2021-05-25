package com.acuant.acuantcamera.helper

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

        if (mrzLines.size == 2) {
            //Log.d("MRZLOG", "Follows:\n" + mrzLines[0] + "\n" +mrzLines[1])
            var result = parseFirstLineOfTwoLine(mrzLines[0])
            //not checking result of first line as it is not strictly needed for echip reading
            result = parseSecondLineOfTwoLine(mrzLines[1], result)

            result?.threeLineMrz = false
            //Log.d("MRZLOG", " " + result?.checkSumResult1 + ", "+ result?.checkSumResult2 +", "+ result?.checkSumResult3 +", "+ result?.checkSumResult4 +", "+ result?.checkSumResult5)
            return result
        } else if (mrzLines.size == 3) {
            //Log.d("MRZLOG", "Follows:\n" + mrzLines[0] + "\n" +mrzLines[1])
            var result = parseFirstLineOfThreeLine(mrzLines[0])
            if (result != null) {
                result = parseSecondLineOfThreeLine(mrzLines[1], result)

                result?.threeLineMrz = true

                return result
                //third line does not contain any relevant info
            }
        }
        return null
    }

    private fun parseFirstLineOfThreeLine (firstLine: String) : MrzResult? {
        var startPos = 0
        if(firstLine.length == 30){
            startPos+=2

            val country = firstLine.substring(startPos, startPos+3)
            startPos+=3

            var passportNumber = firstLine.substring(startPos, startPos+9)
            startPos+=9

            var check1 = firstLine[startPos]
            var checkSumResult1 = checkSum(passportNumber, check1)
            ++startPos

            if (!checkSumResult1) {
                passportNumber = passportNumber.replace('O', '0')
                checkSumResult1 = checkSum(passportNumber, check1)

                if (checkSumResult1 && check1 == 'O') {
                    check1 = '0'
                    checkSumResult1 = checkSum(passportNumber, check1)
                }
            }

            val optional1 = firstLine.substring(startPos, startPos + 15)

            return MrzResult(country = country, passportNumber = passportNumber, checkSumResult1 = checkSumResult1, checkChar1 = check1, optionalField1 = optional1)
        }
        return null
    }

    private fun parseSecondLineOfThreeLine (line: String, result: MrzResult) : MrzResult? {

        if(line.length != 30){
            return null
        }

        var startPos = 0
        result.dob = line.substring(startPos, startPos+6)
        startPos+=6

        var check2 = line[startPos]
        result.checkSumResult2 = checkSum(result.dob, check2)
        ++startPos

        if (!result.checkSumResult2) {
            result.dob = result.dob.replace('O', '0')
            result.checkSumResult2 = checkSum(result.dob, check2)

            if (!result.checkSumResult2 && check2 == 'O') {
                check2 = '0'
                result.checkSumResult2 = checkSum(result.dob, check2)
            }
        }

        result.gender = line.substring(startPos, startPos+1)
        ++startPos

        result.passportExpiration = line.substring(startPos, startPos+6)
        startPos+=6

        var check3 = line[startPos]
        result.checkSumResult3 = checkSum(result.passportExpiration, check3)
        ++startPos

        if (!result.checkSumResult3) {
            result.passportExpiration = result.passportExpiration.replace('O', '0')
            result.checkSumResult3 = checkSum(result.passportExpiration, check3)

            if (!result.checkSumResult3 && check3 == 'O') {
                check3 = '0'
                result.checkSumResult3 = checkSum(result.passportExpiration, check3)
            }
        }

        result.nationality = line.substring(startPos, startPos+3)
        startPos+=3

        val optional2 = line.substring(startPos, startPos+11)
        startPos += 11

        val finalCheckString = result.passportNumber + result.checkChar1 + result.optionalField1 + result.dob + check2 +
                result.passportExpiration + check3 + optional2
        result.checkSumResult4 = checkSum(finalCheckString, line[startPos])

        if (!result.checkSumResult4 && line[startPos] == 'O') {
            result.checkSumResult4 = checkSum(finalCheckString, '0')
        }

        result.checkSumResult5 = true

        result.passportNumber = result.passportNumber.replace("<", "")

        return result
    }


    private fun parseFirstLineOfTwoLine(firstLine:String): MrzResult?{
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

    private fun parseSecondLineOfTwoLine (line: String, result: MrzResult?) : MrzResult? {


        if(line.length != 44){
            return null
        }

        var resultLocal = result

        if(resultLocal == null) {
            resultLocal = MrzResult()
        }

        var startPos = 0
        resultLocal.passportNumber = line.substring(startPos, startPos+9)//.replace("<", "")
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
            resultLocal.checkSumResult5 = checkSum(finalCheckString, '0')
        }

        resultLocal.passportNumber = resultLocal.passportNumber.replace("<", "")

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