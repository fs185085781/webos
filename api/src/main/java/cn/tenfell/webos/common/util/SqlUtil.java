package cn.tenfell.webos.common.util;

import cn.hutool.core.lang.Assert;

import java.util.ArrayList;
import java.util.List;

public class SqlUtil {
    public static List<String> splitSqlScript(String sql) {
        List<String> list = new ArrayList<>();
        splitSqlScript(sql, ";", list);
        return list;
    }

    public static void splitSqlScript(String script, String separator, List<String> statements) {
        splitSqlScript(null, script, separator, "--", "/*",
                "*/", statements);
    }

    public static void splitSqlScript(String resource, String script,
                                      String separator, String commentPrefix, String blockCommentStartDelimiter,
                                      String blockCommentEndDelimiter, List<String> statements) {

        Assert.isTrue(hasText(commentPrefix), "'commentPrefix' must not be null or empty");
        splitSqlScript(resource, script, separator, new String[]{commentPrefix},
                blockCommentStartDelimiter, blockCommentEndDelimiter, statements);
    }

    public static void splitSqlScript(String resource, String script,
                                      String separator, String[] commentPrefixes, String blockCommentStartDelimiter,
                                      String blockCommentEndDelimiter, List<String> statements) {

        Assert.isTrue(hasText(script), "'script' must not be null or empty");
        Assert.notNull(separator, "'separator' must not be null");
        Assert.notEmpty(commentPrefixes, "'commentPrefixes' must not be null or empty");
        for (String commentPrefix : commentPrefixes) {
            Assert.isTrue(hasText(commentPrefix), "'commentPrefixes' must not contain null or empty elements");
        }
        Assert.isTrue(hasText(blockCommentStartDelimiter), "'blockCommentStartDelimiter' must not be null or empty");
        Assert.isTrue(hasText(blockCommentEndDelimiter), "'blockCommentEndDelimiter' must not be null or empty");

        StringBuilder sb = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inEscape = false;

        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            if (inEscape) {
                inEscape = false;
                sb.append(c);
                continue;
            }
            // MySQL style escapes
            if (c == '\\') {
                inEscape = true;
                sb.append(c);
                continue;
            }
            if (!inDoubleQuote && (c == '\'')) {
                inSingleQuote = !inSingleQuote;
            } else if (!inSingleQuote && (c == '"')) {
                inDoubleQuote = !inDoubleQuote;
            }
            if (!inSingleQuote && !inDoubleQuote) {
                if (script.startsWith(separator, i)) {
                    // We've reached the end of the current statement
                    if (sb.length() > 0) {
                        statements.add(sb.toString());
                        sb = new StringBuilder();
                    }
                    i += separator.length() - 1;
                    continue;
                } else if (startsWithAny(script, commentPrefixes, i)) {
                    // Skip over any content from the start of the comment to the EOL
                    int indexOfNextNewline = script.indexOf('\n', i);
                    if (indexOfNextNewline > i) {
                        i = indexOfNextNewline;
                        continue;
                    } else {
                        // If there's no EOL, we must be at the end of the script, so stop here.
                        break;
                    }
                } else if (script.startsWith(blockCommentStartDelimiter, i)) {
                    // Skip over any block comments
                    int indexOfCommentEnd = script.indexOf(blockCommentEndDelimiter, i);
                    if (indexOfCommentEnd > i) {
                        i = indexOfCommentEnd + blockCommentEndDelimiter.length() - 1;
                        continue;
                    } else {
                        throw new RuntimeException(
                                "Missing block comment end delimiter: " + blockCommentEndDelimiter);
                    }
                } else if (c == ' ' || c == '\r' || c == '\n' || c == '\t') {
                    // Avoid multiple adjacent whitespace characters
                    if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                        c = ' ';
                    } else {
                        continue;
                    }
                }
            }
            sb.append(c);
        }

        if (hasText(sb)) {
            statements.add(sb.toString());
        }
    }

    private static boolean startsWithAny(String script, String[] prefixes, int offset) {
        for (String prefix : prefixes) {
            if (script.startsWith(prefix, offset)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasText(CharSequence str) {
        return str != null && str.length() > 0 && containsText(str);
    }

    private static boolean containsText(CharSequence str) {
        int strLen = str.length();

        for (int i = 0; i < strLen; ++i) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }

        return false;
    }
}
