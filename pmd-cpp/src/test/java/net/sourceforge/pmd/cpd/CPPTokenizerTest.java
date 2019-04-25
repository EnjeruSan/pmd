/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.cpd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.lang.ast.TokenMgrError;

public class CPPTokenizerTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testUTFwithBOM() {
        Tokens tokens = parse("\ufeffint start()\n{ int ret = 1;\nreturn ret;\n}\n");
        assertNotSame(TokenEntry.getEOF(), tokens.getTokens().get(0));
        assertEquals(15, tokens.size());
    }

    @Test
    public void testUnicodeSupport() {
        String code = "\ufeff" + "#include <iostream>\n" + "#include <string>\n" + "\n" + "// example\n" + "\n"
                + "int main()\n" + "{\n" + "    std::string text(\"ąęćśźńó\");\n" + "    std::cout << text;\n"
                + "    return 0;\n" + "}\n";
        Tokens tokens = parse(code);
        assertNotSame(TokenEntry.getEOF(), tokens.getTokens().get(0));
        assertEquals(24, tokens.size());
    }
    
    @Test
    public void testIgnoreBetweenSpecialComments() {
        String code = "#include <iostream>\n" + "#include <string>\n" + "\n" + "// CPD-OFF\n"
                + "int main()\n" + "{\n" + "    std::string text(\"ąęćśźńó\");\n" + "    std::cout << text;\n"
                + "    return 0;\n" + "// CPD-ON\n" + "}\n";
        Tokens tokens = parse(code);
        assertNotSame(TokenEntry.getEOF(), tokens.getTokens().get(0));
        assertEquals(2, tokens.size()); // "}" + EOF
    }

    @Test
    public void testMultiLineMacros() {
        Tokens tokens = parse(TEST1);
        assertEquals(7, tokens.size());
    }

    @Test
    public void testDollarSignInIdentifier() {
        parse(TEST2);
    }

    @Test
    public void testDollarSignStartingIdentifier() {
        parse(TEST3);
    }

    @Test
    public void testWideCharacters() {
        parse(TEST4);
    }

    @Test
    public void testTokenizerWithSkipBlocks() throws Exception {
        String test = IOUtils.toString(CPPTokenizerTest.class.getResourceAsStream("cpp/cpp_with_asm.cpp"), StandardCharsets.UTF_8);
        Tokens tokens = parse(test, true, new Tokens());
        assertEquals(19, tokens.size());
    }

    @Test
    public void testTokenizerWithSkipBlocksPattern() throws Exception {
        String test = IOUtils.toString(CPPTokenizerTest.class.getResourceAsStream("cpp/cpp_with_asm.cpp"), StandardCharsets.UTF_8);
        Tokens tokens = new Tokens();
        try {
            parse(test, true, "#if debug|#endif", tokens);
        } catch (TokenMgrError ignored) {
            // ignored
        }
        assertEquals(31, tokens.size());
    }

    @Test
    public void testTokenizerWithoutSkipBlocks() throws Exception {
        String test = IOUtils.toString(CPPTokenizerTest.class.getResourceAsStream("cpp/cpp_with_asm.cpp"), StandardCharsets.UTF_8);
        Tokens tokens = new Tokens();
        try {
            parse(test, false, tokens);
        } catch (TokenMgrError ignored) {
            // ignored
        }
        assertEquals(37, tokens.size());
    }

    @Test
    // ASM code containing the '@' character
    public void testAsmWithAtSign() {
        Tokens tokens = parse(TEST7);
        assertEquals(22, tokens.size());
    }

    @Test
    public void testEOLCommentInPreprocessingDirective() {
        parse("#define LSTFVLES_CPP  //*" + PMD.EOL);
    }

    @Test
    public void testEmptyCharacter() {
        Tokens tokens = parse("std::wstring wsMessage( sMessage.length(), L'');" + PMD.EOL);
        assertEquals(15, tokens.size());
    }

    @Test
    public void testHexCharacter() {
        Tokens tokens = parse("if (*pbuf == '\\0x05')" + PMD.EOL);
        assertEquals(8, tokens.size());
    }

    @Test
    public void testWhiteSpaceEscape() {
        Tokens tokens = parse("szPath = m_sdcacheDir + _T(\"\\    oMedia\");" + PMD.EOL);
        assertEquals(10, tokens.size());
    }

    @Test
    public void testRawStringLiteral() {
        String code = "const char* const KDefaultConfig = R\"(\n" + "    [Sinks.1]\n" + "    Destination=Console\n"
                + "    AutoFlush=true\n"
                + "    Format=\"[%TimeStamp%] %ThreadId% %QueryIdHigh% %QueryIdLow% %LoggerFile%:%Line% (%Severity%) - %Message%\"\n"
                + "    Filter=\"%Severity% >= WRN\"\n" + ")\";\n";
        Tokens tokens = parse(code);
        assertTrue(TokenEntry.getEOF() != tokens.getTokens().get(0));
        assertEquals(9, tokens.size());
    }

    @Test
    public void testLexicalErrorFilename() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(Tokenizer.OPTION_SKIP_BLOCKS, Boolean.toString(false));
        String test = IOUtils.toString(CPPTokenizerTest.class.getResourceAsStream("cpp/issue-1559.cpp"), StandardCharsets.UTF_8);
        SourceCode code = new SourceCode(new SourceCode.StringCodeLoader(test, "issue-1559.cpp"));
        CPPTokenizer tokenizer = new CPPTokenizer();
        tokenizer.setProperties(properties);

        expectedException.expect(TokenMgrError.class);
        expectedException.expectMessage("Lexical error in file issue-1559.cpp at");
        tokenizer.tokenize(code, new Tokens());
    }

    private Tokens parse(String snippet) {
        try {
            return parse(snippet, false, new Tokens());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Tokens parse(String snippet, boolean skipBlocks, Tokens tokens) throws IOException {
        return parse(snippet, skipBlocks, null, tokens);
    }

    private Tokens parse(String snippet, boolean skipBlocks, String skipPattern, Tokens tokens) throws IOException {
        Properties properties = new Properties();
        properties.setProperty(Tokenizer.OPTION_SKIP_BLOCKS, Boolean.toString(skipBlocks));
        if (skipPattern != null) {
            properties.setProperty(Tokenizer.OPTION_SKIP_BLOCKS_PATTERN, skipPattern);
        }

        CPPTokenizer tokenizer = new CPPTokenizer();
        tokenizer.setProperties(properties);

        SourceCode code = new SourceCode(new SourceCode.StringCodeLoader(snippet));
        tokenizer.tokenize(code, tokens);
        return tokens;
    }

    private static final String TEST1 = "#define FOO a +\\" + PMD.EOL + "            b +\\" + PMD.EOL
            + "            c +\\" + PMD.EOL + "            d +\\" + PMD.EOL + "            e +\\" + PMD.EOL
            + "            f +\\" + PMD.EOL + "            g" + PMD.EOL + " void main() {}";

    private static final String TEST2 = " void main() { int x$y = 42; }";

    private static final String TEST3 = " void main() { int $x = 42; }";

    private static final String TEST4 = " void main() { char x = L'a'; }";

    private static final String TEST7 = "asm void eSPI_boot()" + PMD.EOL + "{" + PMD.EOL + "  // setup stack pointer"
            + PMD.EOL + "  lis r1, _stack_addr@h" + PMD.EOL + "  ori r1, r1, _stack_addr@l" + PMD.EOL + "}";
}
