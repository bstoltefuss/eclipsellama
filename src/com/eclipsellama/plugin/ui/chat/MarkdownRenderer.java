package com.eclipsellama.plugin.ui.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

/**
 * Parses markdown content and generates SWT StyleRanges for rendering.
 * Supports: code blocks, inline code, bold, italic.
 */
public class MarkdownRenderer {

    // Regex patterns
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile(
            "```(\\w*)\\n([\\s\\S]*?)```", Pattern.MULTILINE);
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*([^*]+)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*([^*]+)\\*");

    private final ChatStyles styles;

    public MarkdownRenderer(ChatStyles styles) {
        this.styles = styles;
    }

    /**
     * Parsed segment of text with its style.
     */
    public static class StyledSegment {
        public final String text;
        public final SegmentType type;
        public final String language; // For code blocks

        public StyledSegment(String text, SegmentType type) {
            this(text, type, null);
        }

        public StyledSegment(String text, SegmentType type, String language) {
            this.text = text;
            this.type = type;
            this.language = language;
        }
    }

    public enum SegmentType {
        NORMAL,
        CODE_BLOCK,
        INLINE_CODE,
        BOLD,
        ITALIC
    }

    /**
     * Parse markdown and return styled segments.
     */
    public List<StyledSegment> parse(String markdown) {
        List<StyledSegment> segments = new ArrayList<>();

        if (markdown == null || markdown.isEmpty()) {
            return segments;
        }

        // First extract code blocks (highest priority)
        Matcher codeBlockMatcher = CODE_BLOCK_PATTERN.matcher(markdown);
        int lastEnd = 0;

        while (codeBlockMatcher.find()) {
            // Add text before code block
            if (codeBlockMatcher.start() > lastEnd) {
                String before = markdown.substring(lastEnd, codeBlockMatcher.start());
                parseInlineStyles(before, segments);
            }

            // Add code block
            String language = codeBlockMatcher.group(1);
            String code = codeBlockMatcher.group(2);
            segments.add(new StyledSegment(code, SegmentType.CODE_BLOCK, language));

            lastEnd = codeBlockMatcher.end();
        }

        // Add remaining text after last code block
        if (lastEnd < markdown.length()) {
            String remaining = markdown.substring(lastEnd);
            parseInlineStyles(remaining, segments);
        }

        // If no code blocks found, parse entire text
        if (segments.isEmpty()) {
            parseInlineStyles(markdown, segments);
        }

        return segments;
    }

    /**
     * Parse inline styles (inline code, bold, italic).
     */
    private void parseInlineStyles(String text, List<StyledSegment> segments) {
        // Simple approach: process inline code first, then add as normal
        // For more complex parsing, would need proper tokenizer

        Matcher inlineCodeMatcher = INLINE_CODE_PATTERN.matcher(text);
        int lastEnd = 0;

        while (inlineCodeMatcher.find()) {
            // Text before inline code
            if (inlineCodeMatcher.start() > lastEnd) {
                String before = text.substring(lastEnd, inlineCodeMatcher.start());
                parseBoldItalic(before, segments);
            }

            // Inline code
            String code = inlineCodeMatcher.group(1);
            segments.add(new StyledSegment(code, SegmentType.INLINE_CODE));

            lastEnd = inlineCodeMatcher.end();
        }

        // Remaining text
        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd);
            parseBoldItalic(remaining, segments);
        }

        // If nothing found, add plain
        if (lastEnd == 0 && !text.isEmpty()) {
            parseBoldItalic(text, segments);
        }
    }

    /**
     * Parse bold and italic in text.
     */
    private void parseBoldItalic(String text, List<StyledSegment> segments) {
        Matcher boldMatcher = BOLD_PATTERN.matcher(text);
        int lastEnd = 0;

        while (boldMatcher.find()) {
            if (boldMatcher.start() > lastEnd) {
                segments.add(new StyledSegment(
                        text.substring(lastEnd, boldMatcher.start()),
                        SegmentType.NORMAL));
            }

            segments.add(new StyledSegment(boldMatcher.group(1), SegmentType.BOLD));
            lastEnd = boldMatcher.end();
        }

        if (lastEnd < text.length()) {
            segments.add(new StyledSegment(text.substring(lastEnd), SegmentType.NORMAL));
        }

        if (lastEnd == 0 && !text.isEmpty()) {
            segments.add(new StyledSegment(text, SegmentType.NORMAL));
        }
    }

    /**
     * Create StyleRange for a segment at a specific offset.
     */
    public StyleRange createStyleRange(StyledSegment segment, int startOffset) {
        StyleRange range = new StyleRange();
        range.start = startOffset;
        range.length = segment.text.length();

        switch (segment.type) {
            case CODE_BLOCK:
                range.background = styles.getCodeBackground();
                range.foreground = styles.getCodeForeground();
                range.font = styles.getCodeFont();
                break;

            case INLINE_CODE:
                range.background = styles.getCodeBackground();
                range.foreground = styles.getCodeForeground();
                range.font = styles.getCodeFont();
                break;

            case BOLD:
                range.fontStyle = SWT.BOLD;
                break;

            case ITALIC:
                range.fontStyle = SWT.ITALIC;
                break;

            case NORMAL:
            default:
                // No special styling
                return null;
        }

        return range;
    }

    /**
     * Check if text contains code blocks.
     */
    public boolean hasCodeBlocks(String text) {
        return CODE_BLOCK_PATTERN.matcher(text).find();
    }

    /**
     * Extract all code blocks from text.
     */
    public List<CodeBlock> extractCodeBlocks(String text) {
        List<CodeBlock> blocks = new ArrayList<>();
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(text);

        while (matcher.find()) {
            String language = matcher.group(1);
            String code = matcher.group(2);
            blocks.add(new CodeBlock(language, code, matcher.start(), matcher.end()));
        }

        return blocks;
    }

    /**
     * Represents a code block in text.
     */
    public static class CodeBlock {
        public final String language;
        public final String code;
        public final int startIndex;
        public final int endIndex;

        public CodeBlock(String language, String code, int startIndex, int endIndex) {
            this.language = language;
            this.code = code;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }
}
