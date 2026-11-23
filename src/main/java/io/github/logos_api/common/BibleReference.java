package io.github.logos_api.common;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "요3:16", "요한복음 3:16-18", "요3"처럼 사람이 쓰는 성경 참조 표기를 나눠 담는 값 객체입니다.
 * <p>
 * 책 이름은 여기서 정식 이름으로 바꾸지 않습니다. 약어 표를 가진 쪽에서 해석합니다.
 * </p>
 */
public class BibleReference {

    private static final Pattern REFERENCE_PATTERN =
            Pattern.compile("^(?<book>.+?)\\s*(?<chapter>\\d+)(?::(?<from>\\d+)(?:-(?<to>\\d+))?)?$");

    private final String book;
    private final int chapter;
    private final Integer fromVerse;
    private final Integer toVerse;

    /**
     * 나눈 값으로 참조를 만듭니다. {@link #parse(String)}만 호출합니다.
     *
     * @param book      책 이름 또는 약어
     * @param chapter   장 번호
     * @param fromVerse 시작 절. 장 전체이면 null
     * @param toVerse   끝 절. 한 절이거나 장 전체이면 null
     */
    private BibleReference(String book, int chapter, Integer fromVerse, Integer toVerse) {
        this.book = book;
        this.chapter = chapter;
        this.fromVerse = fromVerse;
        this.toVerse = toVerse;
    }

    /**
     * 참조 문자열을 나눕니다. 형식에 맞지 않으면 빈 값을 돌려줍니다.
     *
     * @param reference "요3:16", "요한복음 3:16-18", "요3" 형태의 문자열
     * @return 나눠 담은 참조. 형식이 맞지 않으면 {@link Optional#empty()}
     */
    public static Optional<BibleReference> parse(String reference) {
        if (reference == null || reference.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = REFERENCE_PATTERN.matcher(reference.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String book = matcher.group("book").trim();
        if (book.isEmpty()) {
            return Optional.empty();
        }

        int chapter = Integer.parseInt(matcher.group("chapter"));
        Integer from = matcher.group("from") == null ? null : Integer.parseInt(matcher.group("from"));
        Integer to = matcher.group("to") == null ? null : Integer.parseInt(matcher.group("to"));

        if (chapter < 1 || (from != null && from < 1) || (to != null && to < from)) {
            return Optional.empty();
        }

        return Optional.of(new BibleReference(book, chapter, from, to));
    }

    /**
     * 참조에 적힌 책 이름을 반환합니다. 정식 이름일 수도, 약어일 수도 있습니다.
     *
     * @return 책 이름 또는 약어
     */
    public String getBook() {
        return book;
    }

    /**
     * 참조의 장 번호를 반환합니다.
     *
     * @return 장 번호
     */
    public int getChapter() {
        return chapter;
    }

    /**
     * 절 번호를 지정하지 않은 장 전체 참조인지 확인합니다.
     *
     * @return "요3"처럼 절이 없으면 true
     */
    public boolean isWholeChapter() {
        return fromVerse == null;
    }

    /**
     * 참조에 포함된 절 번호인지 확인합니다. 장 전체 참조이면 항상 true입니다.
     *
     * @param verse 확인할 절 번호
     * @return 범위에 들어가면 true
     */
    public boolean contains(int verse) {
        if (isWholeChapter()) {
            return true;
        }
        int last = toVerse == null ? fromVerse : toVerse;
        return verse >= fromVerse && verse <= last;
    }

    /**
     * 범위의 첫 절 번호를 반환합니다. 장 전체 참조이면 1입니다.
     *
     * @return 시작 절 번호
     */
    public int firstVerse() {
        return fromVerse == null ? 1 : fromVerse;
    }
}
