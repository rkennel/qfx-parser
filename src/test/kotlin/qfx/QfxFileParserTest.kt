package qfx

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class QfxFileParserTest {

    @Test
    fun parsesHeadersCorrectly() {

        val qfxFileText = """

            OFXHEADER:100
            DATA:OFXSGML
            VERSION:102
            SECURITY:NONE
            ENCODING:USASCII
            CHARSET:1252
            COMPRESSION:NONE
            OLDFILEUID:NONE
            NEWFILEUID:NONE
            <OFX>
            </OFX>
        """.trimIndent()

        val headers = qfx.parseHeaders(qfxFileText)

        assertThat(headers.size).isGreaterThan(0)
        assertThat(headers["DATA"]).isEqualTo("OFXSGML")
        assertThat(headers["VERSION"]).isEqualTo("102")
        assertThat(headers["SECURITY"]).isEqualTo("NONE")
        assertThat(headers["ENCODING"]).isEqualTo("USASCII")
        assertThat(headers["CHARSET"]).isEqualTo("1252")
        assertThat(headers["COMPRESSION"]).isEqualTo("NONE")
        assertThat(headers["OLDFILEUID"]).isEqualTo("NONE")
        assertThat(headers["NEWFILEUID"]).isEqualTo("NONE")
    }

    @Test
    fun parsesServerConnectionInfoCorrectly(){

        val strictConnectionInfo = """
            <SIGNONMSGSRSV1>
                <SONRS>
                    <STATUS>
                        <CODE>0</CODE>
                        <SEVERITY>INFO</SEVERITY>
                        <MESSAGE>SUCCESS</MESSAGE>
                    </STATUS>
                    <DTSERVER>20190105170625.000[0:GMT]</DTSERVER>
                    <LANGUAGE>ENG</LANGUAGE>
                    <FI>
                        <ORG>Target</ORG>
                        <FID>3820</FID>
                    </FI>
                    <INTU.BID>3820</INTU.BID>
                    <INTU.USERID>Target</INTU.USERID>
                </SONRS>
            </SIGNONMSGSRSV1>
        """.trimIndent()

        val serverConnectionInfo = qfx.parseServerConnectionInfo(strictConnectionInfo)
        assertThat(serverConnectionInfo.status).isEqualTo(QfxServerConnectionStatus("0","INFO","SUCCESS"))
        assertThat(serverConnectionInfo.serverDate.toLocalDate()).isEqualTo(LocalDate.of(2019,1,5))
        assertThat(serverConnectionInfo.language).isEqualTo("ENG")
        assertThat(serverConnectionInfo.bankInformation).isEqualTo(QfxBankInformation("Target","3820","3820","Target"))
    }

    @Test
    fun parsesLooseServerConnectionInfoCorrectly(){

        val strictConnectionInfo = """
            <SIGNONMSGSRSV1>
            <SONRS>
            <STATUS>
            <CODE>0
            <SEVERITY>INFO
            </STATUS>
            <DTSERVER>20190105120000[0:GMT]
            <LANGUAGE>ENG
            <FI>
            <ORG>B1
            <FID>10898
            </FI>
            <INTU.BID>10898
            </SONRS>
            </SIGNONMSGSRSV1>
        """.trimIndent()

        val serverConnectionInfo = qfx.parseServerConnectionInfo(strictConnectionInfo)
        assertThat(serverConnectionInfo.status).isEqualTo(QfxServerConnectionStatus("0","INFO",""))
        assertThat(serverConnectionInfo.serverDate.toLocalDate()).isEqualTo(LocalDate.of(2019,1,5))
        assertThat(serverConnectionInfo.language).isEqualTo("ENG")
        assertThat(serverConnectionInfo.bankInformation).isEqualTo(QfxBankInformation("B1","10898","10898",""))
    }

    @Test
    fun parsesQfxServerConnectionStatusCorrectly() {

        val strictStatus = """
            <STATUS>
                <CODE>0</CODE>
                <SEVERITY>INFO</SEVERITY>
                <MESSAGE>SUCCESS</MESSAGE>
            </STATUS>
        """.trimIndent()

        val status = qfx.parseStatus(strictStatus)
        assertThat(status.code).isEqualTo("0")
        assertThat(status.severity).isEqualTo("INFO")
        assertThat(status.message).isEqualTo("SUCCESS")
    }

    @Test
    fun parsesLooseStatusCorrectly() {

        val looseStatus = """
            <STATUS>
            <CODE>0
            <SEVERITY>INFO
            </STATUS>
        """.trimIndent()

        val status = qfx.parseStatus(looseStatus)
        assertThat(status.code).isEqualTo("0")
        assertThat(status.severity).isEqualTo("INFO")
        assertThat(status.message).isEqualTo("")
    }

}