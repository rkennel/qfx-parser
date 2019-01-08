package qfx

import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class QfxFile(val headers: Map<String, String>)

data class QfxServerConnectionInfo(
    val status: QfxStatus,
    val serverDate: QfxServerDate,
    val language: String,
    val bankInformation: QfxBankInformation
) {

}

data class QfxStatus(val code: String, val severity: String, val message: String)

class QfxServerDate(val dateText: String) {
    fun toLocalDate(): LocalDate {
        return LocalDate.parse(dateText.substring(0, 8), DateTimeFormatter.ofPattern("yyyyMMdd"))
    }

}

data class QfxBankInformation(val org: String, val fid: String, val bid: String, val userId: String)

data class QfxCreditCardStatement(
    val trnuid: String,
    val status: QfxStatus,
    val defaultCurrency: String,
    val accountId: String,
    val accountKey: String,
    val beginDate: LocalDateTime,
    val endDate: LocalDateTime,
    val transactions: List<QfxTransaction>
)

data class QfxTransaction (val type:String)

fun parseFile(filename: String): QfxFile {
    val fileText: String = File(filename).readText(Charsets.UTF_8)

    return QfxFile(parseHeaders(fileText))
}

internal fun parseHeaders(fileText: String): Map<String, String> {

    val headers = mutableMapOf<String, String>()

    val headersSection = fileText.substring(0, fileText.indexOf('<') - 1)

    headersSection.split("\n").map { line ->
        if (line.indexOf(':') > 1) {
            val arrHeader = line.split(":")
            headers[arrHeader[0]] = arrHeader[1]
        }
    }

    return headers
}

internal fun parseServerConnectionInfo(serverConnectionStatus: String): QfxServerConnectionInfo {

    val statusText = getElementAsText(serverConnectionStatus, "STATUS")
    val qfxServerConnectionStatus = parseStatus(statusText)

    val qfxServerDate = QfxServerDate(valueAfterTag(serverConnectionStatus, "DTSERVER"))

    val language = valueAfterTag(serverConnectionStatus, "LANGUAGE")

    val bankInformation = parseBankInformation(serverConnectionStatus)

    return QfxServerConnectionInfo(qfxServerConnectionStatus, qfxServerDate, language, bankInformation)
}

internal fun parseStatus(statusText: String): QfxStatus {
    val code = valueAfterTag(statusText, "CODE")
    val severity = valueAfterTag(statusText, "SEVERITY")
    val message = valueAfterTag(statusText, "MESSAGE")

    return QfxStatus(code, severity, message)
}

internal fun parseBankInformation(serverConnectionStatus: String): QfxBankInformation {
    return QfxBankInformation(
        valueAfterTag(serverConnectionStatus, "ORG"),
        valueAfterTag(serverConnectionStatus, "FID"),
        valueAfterTag(serverConnectionStatus, "INTU.BID"),
        valueAfterTag(serverConnectionStatus, "INTU.USERID")
    )
}

internal fun getElementAsText(textWithElements: String, elementName: String): String {
    val beginElement = "<$elementName>"
    val endElement = "</$elementName>"

    val indexAtBeginOfElement = textWithElements.indexOf(beginElement)
    val indexAtEndOfElement = textWithElements.indexOf(endElement) + endElement.length

    return textWithElements.substring(indexAtBeginOfElement, indexAtEndOfElement)
}


private fun valueAfterTag(textWithTags: String, tagName: String): String {

    val tag = "<" + tagName + ">"

    val indexAtBeginOfTag = textWithTags.indexOf(tag)

    if (indexAtBeginOfTag == -1) {
        return ""
    }

    val beginIndex = indexAtBeginOfTag + tag.length
    val endIndex = textWithTags.indexOf("<", indexAtBeginOfTag + 1)

    return textWithTags.substring(beginIndex, endIndex).trim()
}

internal fun parseCreditCardStatement(creditCardStatementText: String): QfxCreditCardStatement {
    val trnuid = valueAfterTag(creditCardStatementText, "TRNUID")
    val status = parseStatus(getElementAsText(creditCardStatementText, "STATUS"))

    val creditCardStatementResponseText = getElementAsText(creditCardStatementText, "CCSTMTRS")
    val defaultCurrency = valueAfterTag(creditCardStatementResponseText, "CURDEF")

    val ccacctfromText = getElementAsText(creditCardStatementText, "CCACCTFROM")
    val accountId = valueAfterTag(ccacctfromText, "ACCTID")
    val accountKey = valueAfterTag(ccacctfromText, "ACCTKEY")

    val bankTransListText = getElementAsText(creditCardStatementResponseText, "BANKTRANLIST")
    val beginDate = parseStatementDateTime(valueAfterTag(bankTransListText, "DTSTART"))
    val endDate = parseStatementDateTime(valueAfterTag(bankTransListText, "DTEND"))

    val transactions = parseTransactions(creditCardStatementResponseText)

    return QfxCreditCardStatement(trnuid, status, defaultCurrency, accountId, accountKey, beginDate, endDate, transactions)
}

private fun parseStatementDateTime(dateText: String): LocalDateTime {
    val year = dateText.substring(0,4).toInt()
    val month = dateText.substring(4,6).toInt()
    val day = dateText.substring(6,8).toInt()
    val hour = dateText.substring(8,10).toInt()
    val minute = dateText.substring(10,12).toInt()
    val second = dateText.substring(12,14).toInt()

    return LocalDateTime.of(year,month,day,hour,minute,second)
}

internal fun parseTransactions(creditCardStatementText: String) : List<QfxTransaction>{

    val transactionList = mutableListOf<QfxTransaction>()

    var currentIndex = 0
    var endOfText=false
    while (!endOfText){
        val beginIndex = creditCardStatementText.indexOf("<STMTTRN>",currentIndex)
        val endIndex = creditCardStatementText.indexOf("</STMTTRN>",beginIndex)+"</STMTTRN>".length

        if(beginIndex!=-1&&endIndex!=-1){
            transactionList.add(parseTransaction(creditCardStatementText.substring(beginIndex,endIndex)))
            currentIndex=endIndex
        }
        else{
            endOfText=true
        }

    }

    return transactionList

}

internal fun parseTransaction(transactionText:String):QfxTransaction{

    val type = valueAfterTag(transactionText,"TRNTYPE")

    return QfxTransaction(type)

}



