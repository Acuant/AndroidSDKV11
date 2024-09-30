package com.acuant.acuantcamera.helper

class MrzParser {

    companion object {
        private const val FILLER = '<'
        private const val PASSPORT_FIRST_VALUE = 'P'
        private const val CHECK_SUM_ARRAY = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    }

    internal fun parseMrz(mrz:String): MrzResult? {
        val mrzWithNoSpaces = mrz.replace(" ",  "")
        val mrzLinesTmp = mrzWithNoSpaces.split('\n')
        val mrzLines: MutableList<String> = mutableListOf()
        for (i in mrzLinesTmp.indices) {
            if(mrzLinesTmp[i].length > 6) {
                mrzLines.add(mrzLinesTmp[i])
            }
        }
        var result: MrzResult? = null
        if (mrzLines.size == 2) {
            result = parseFirstLineOfTwoLine(mrzLines[0])
            result = parseSecondLineOfTwoLine(mrzLines[1], result)

            result.threeLineMrz = false
        } else if (mrzLines.size == 3) {

            result = parseFirstLineOfThreeLine(mrzLines[0])
            if (result != null) {
                result = parseSecondLineOfThreeLine(mrzLines[1], result)

                result?.threeLineMrz = true
                //third line does not contain any relevant info
            }
        }
        result?.cleanFields(FILLER)
        return result
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
            ++startPos

            //this handles cases where the doc num is longer than 9 which can happen on 3 line
            // documents. In that case the doc num will flow over into the extra data, along
            // with a checksum char followed by a FILLER
            if (check1 == FILLER) {
                val nextFiller = firstLine.indexOf(FILLER, startPos)
                if (nextFiller >= startPos + 2) {
                    passportNumber += firstLine.substring(startPos, nextFiller - 1)
                    check1 = firstLine[nextFiller - 1]
                    startPos = nextFiller
                }
            }
            val subRes = trySubstitutions(passportNumber, check1)

            passportNumber = subRes.first
            check1 = subRes.second

            val checkSumResult1 = checkSum(passportNumber, check1)

            val optional1 = firstLine.substring(startPos)

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

        result.checkChar2 = line[startPos]
        ++startPos

        var subRes = trySubstitutions(result.dob, result.checkChar2)
        result.dob = subRes.first
        result.checkChar2 = subRes.second
        result.checkSumResult2 = checkSum(result.dob, result.checkChar2, mustBeOnlyDigits = true)

        result.gender = line.substring(startPos, startPos+1)
        ++startPos

        result.passportExpiration = line.substring(startPos, startPos+6)
        startPos+=6

        result.checkChar3 = line[startPos]
        ++startPos

        subRes = trySubstitutions(result.passportExpiration, result.checkChar3)
        result.passportExpiration = subRes.first
        result.checkChar3 = subRes.second
        result.checkSumResult3 = checkSum(result.passportExpiration, result.checkChar3, mustBeOnlyDigits = true)

        result.nationality = line.substring(startPos, startPos+3)
        startPos+=3

        val optional2 = line.substring(startPos, startPos+11)
        startPos += 11

        val firstLineForFourthChecksum = if (result.passportNumber.length > 9) {
            result.passportNumber.substring(0, 9) + FILLER + result.passportNumber.substring(9)
        } else {
            result.passportNumber
        } + result.checkChar1 + result.optionalField1

        val finalCheckString = firstLineForFourthChecksum + result.dob + result.checkChar2 +
                result.passportExpiration + result.checkChar3 + optional2
        result.checkChar4 = line[startPos]
        subRes = trySubstitutions(finalCheckString, result.checkChar4, true)
        result.checkChar4 = subRes.second
        result.checkSumResult4 = checkSum(finalCheckString, result.checkChar4)

        result.checkSumResult5 = true

        return result
    }


    private fun parseFirstLineOfTwoLine(firstLine:String): MrzResult{
        var startPos = 0
        if (firstLine[startPos] == PASSPORT_FIRST_VALUE && firstLine.length == 44) {
            startPos+=2

            val country = firstLine.substring(startPos, startPos+3)
            startPos+=3

            var nextPos = firstLine.indexOf("$FILLER$FILLER", startPos)
            val surname = firstLine.substring(startPos, nextPos)
            startPos = nextPos+2

            nextPos = firstLine.indexOf("$FILLER$FILLER", startPos)
            if (nextPos < startPos) {
                nextPos = firstLine.length - 1
            }
            val givenName = firstLine.substring(startPos, nextPos)
            return MrzResult(surName = surname, givenName = givenName, country = country)
        }
        return MrzResult()
    }

    private fun parseSecondLineOfTwoLine (line: String, result: MrzResult) : MrzResult {
        if(line.length != 44){
            return result
        }

        var startPos = 0
        result.passportNumber = line.substring(startPos, startPos+9)
        startPos+=9

        result.checkChar1 = line[startPos]
        ++startPos

        var subRes = trySubstitutions(result.passportNumber, result.checkChar1)
        result.passportNumber = subRes.first
        result.checkChar1 = subRes.second
        result.checkSumResult1 = checkSum(result.passportNumber, result.checkChar1)

        result.nationality = line.substring(startPos, startPos+3)
        startPos+=3

        result.dob = line.substring(startPos, startPos+6)
        startPos+=6

        result.checkChar2 = line[startPos]
        ++startPos

        subRes = trySubstitutions(result.dob, result.checkChar2)
        result.dob = subRes.first
        result.checkChar2 = subRes.second
        result.checkSumResult2 = checkSum(result.dob, result.checkChar2, mustBeOnlyDigits = true)

        result.gender = line.substring(startPos, startPos+1)
        ++startPos

        result.passportExpiration = line.substring(startPos, startPos+6)
        startPos+=6

        result.checkChar3 = line[startPos]
        ++startPos

        subRes = trySubstitutions(result.passportExpiration, result.checkChar3)
        result.passportExpiration = subRes.first
        result.checkChar3 = subRes.second
        result.checkSumResult3 = checkSum(result.passportExpiration, result.checkChar3, mustBeOnlyDigits = true)

        result.personalDocNumber = line.substring(startPos, startPos+14)
        startPos+=14

        result.checkChar4= line[startPos]
        ++startPos

        subRes = trySubstitutions(result.personalDocNumber, result.checkChar4)
        result.personalDocNumber = subRes.first
        result.checkChar4 = subRes.second
        result.checkSumResult4 = checkSum(result.personalDocNumber, result.checkChar4)

        val finalCheckString = result.passportNumber + result.checkChar1 + result.dob + result.checkChar2 +
                result.passportExpiration + result.checkChar3 + result.personalDocNumber + result.checkChar4
        result.checkChar5 = line[startPos]


        subRes = trySubstitutions(finalCheckString, result.checkChar5, true)
        result.checkChar5 = subRes.second
        result.checkSumResult5 = checkSum(finalCheckString, result.checkChar5)

        return result
    }

    //this is a very basic technique. We don't want to spend too much processing time + we don't
    // want to cause too many false positives by allowing too many characters to substitute for
    // one another. This list can however get expanded if we get definitive misreads happening on
    // specific characters
    private fun trySubstitutions(phrase: String, checksum: Char, onlyModifyChecksum: Boolean = false): Pair<String, Char> {

        //numbers should be second in this list as the order of substitutions will try to sub first
        // for second before second for first.
        val subList = listOf(Pair('O', '0'), Pair('S', '5'))

        if (checkSum(phrase, checksum)) {
            return Pair(phrase, checksum)
        }

        for (sub in subList) {

            if (checksum == sub.first && checkSum(phrase, sub.second)) {
                return Pair(phrase, sub.second)
            } else if (checksum == sub.second && checkSum(phrase, sub.first)) {
                return Pair(phrase, sub.first)
            }

            if (onlyModifyChecksum) {
                //for when checking the sum string we don't want to modify it or we can upset previous checksums
                continue
            }

            var modPhrase = phrase.replace(sub.first, sub.second)
            if (checkSum(modPhrase, checksum)) {
                return Pair(modPhrase, checksum)
            } else {
                if (checksum == sub.first && checkSum(modPhrase, sub.second)) {
                    return Pair(modPhrase, sub.second)
                } else if (checksum == sub.second && checkSum(modPhrase, sub.first)) {
                    return Pair(modPhrase, sub.first)
                }
            }
            modPhrase = phrase.replace(sub.second, sub.first)
            if (checkSum(modPhrase, checksum)) {
                return Pair(modPhrase, checksum)
            } else {
                if (checksum == sub.first && checkSum(modPhrase, sub.second)) {
                    return Pair(modPhrase, sub.second)
                } else if (checksum == sub.second && checkSum(modPhrase, sub.first)) {
                    return Pair(modPhrase, sub.first)
                }
            }
        }

        return Pair(phrase, checksum)
    }

    private fun checkSum(input: String, checkSumChar: Char, mustBeOnlyDigits: Boolean = false): Boolean {
        if (mustBeOnlyDigits && !input.matches(Regex("[0-9]+"))) {
            //dob and expiration can only be digits, can short circuit (and prevent false positives)
            return false
        }
        if (!"$checkSumChar".matches(Regex("<|[0-9]"))) {
            //checksum char can only be digits, can short circuit (and prevent false positives)
            return false
        }
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