package qfx

import java.io.File
import java.time.LocalDate
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

data class QfxBankInformation(val org:String, val fid: String, val bid:String, val userId: String)

data class QfxCreditCardStatement(val trnuid:String, val status:QfxStatus, val defaultCurrency: String)

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

    val language = valueAfterTag(serverConnectionStatus,"LANGUAGE")

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
        valueAfterTag(serverConnectionStatus,"ORG"),
        valueAfterTag(serverConnectionStatus,"FID"),
        valueAfterTag(serverConnectionStatus,"INTU.BID"),
        valueAfterTag(serverConnectionStatus,"INTU.USERID")
    )
}

internal fun getElementAsText(textWithElements: String, elementName: String): String {
    val beginElement = "<$elementName>"
    val endElement = "</$elementName>"

    val indexAtBeginOfElement = textWithElements.indexOf(beginElement)
    val indexAtEndOfElement = textWithElements.indexOf(endElement) + endElement.length

    return return textWithElements.substring(indexAtBeginOfElement, indexAtEndOfElement)
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

internal fun parseCreditCardStatement(creditCardStatementText: String) : QfxCreditCardStatement {
    val trnuid = valueAfterTag(creditCardStatementText,"TRNUID")
    val status = parseStatus(getElementAsText(creditCardStatementText,"STATUS"))

    val creditCardStatementResonseText = getElementAsText(creditCardStatementText,"CCSTMTRS")
    val defaultCurrency = valueAfterTag(creditCardStatementResonseText,"CURDEF")

    return QfxCreditCardStatement(trnuid, status,defaultCurrency)
}
