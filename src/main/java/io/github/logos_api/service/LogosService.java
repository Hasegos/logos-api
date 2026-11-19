package io.github.logos_api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.logos_api.common.BibleReference;
import io.github.logos_api.dto.LogosResponseDTO;
import io.github.logos_api.dto.SearchResponseDTO;
import io.github.logos_api.model.Logos;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.json.JsonReadFeature;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 성경 구절 데이터를 로드하고 관리하며 랜덤/오늘의 말씀/책·장·절 조회를 제공하는 서비스 클래스입니다.
 * 애플리케이션 시작 시 JSON 파일들을 메모리에 캐싱하여 빠른 접근을 보장합니다.
 */
@Slf4j
@Service
public class LogosService {

    /**
     * 개역개정 기준 구약 39권의 정경 순서. 실제 등록된 책만 걸러내는 기준으로 사용합니다.
     */
    private static final List<String> OLD_TESTAMENT_ORDER = List.of(
            "창세기", "출애굽기", "레위기", "민수기", "신명기", "여호수아", "사사기", "룻기",
            "사무엘상", "사무엘하", "열왕기상", "열왕기하", "역대상", "역대하", "에스라", "느헤미야",
            "에스더", "욥기", "시편", "잠언", "전도서", "아가", "이사야", "예레미야",
            "예레미야애가", "에스겔", "다니엘", "호세아", "요엘", "아모스", "오바댜", "요나",
            "미가", "나훔", "하박국", "스바냐", "학개", "스가랴", "말라기"
    );

    /**
     * 개역개정 기준 신약 27권의 정경 순서.
     */
    private static final List<String> NEW_TESTAMENT_ORDER = List.of(
            "마태복음", "마가복음", "누가복음", "요한복음", "사도행전", "로마서", "고린도전서",
            "고린도후서", "갈라디아서", "에베소서", "빌립보서", "골로새서", "데살로니가전서",
            "데살로니가후서", "디모데전서", "디모데후서", "디도서", "빌레몬서", "히브리서",
            "야고보서", "베드로전서", "베드로후서", "요한1서", "요한2서", "요한3서",
            "유다서", "요한계시록"
    );

    private static final Pattern VERSE_PATTERN = Pattern.compile("[0-9]+(-[0-9]+)?");

    /**
     * 책 이름 -> 정경 순서 번호. 검색 결과를 성경 순서대로 정렬하는 데 사용합니다.
     */
    private static final Map<String, Integer> BOOK_ORDER = buildBookOrder();

    /**
     * 검색 결과 정렬 기준: 정경 순서 -> 장 -> 절.
     */
    private static final Comparator<Logos> CANONICAL_ORDER =
            Comparator.<Logos, Integer>comparing(v -> BOOK_ORDER.getOrDefault(v.getBook(), Integer.MAX_VALUE))
                    .thenComparingInt(v -> Integer.parseInt(v.getChapter()))
                    .thenComparingInt(v -> verseStart(v.getVerse()));

    private Map<String, Map<Integer, List<Logos>>> bibleMap = new HashMap<>();
    private Map<String, String> bookByAbbrev = Map.of();
    private List<Logos> allVerses = new ArrayList<>();
    private int totalVerseCount;
    private final Random random = new Random();

    /**
     * 클래스 경로 하위의 'bible' 폴더에서 모든 JSON 파일을 읽어 성경 데이터를 초기화합니다.
     * [성경권 -> [장 -> 구절리스트]] 형태의 계층적 Map 구조로 메모리에 적재합니다.
     *
     * @throws IOException 리소스 파일을 읽는 과정에서 오류 발생 시
     */
    @PostConstruct
    public void init() throws IOException{
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);

        try (InputStream abbrevStream = new ClassPathResource("bible-abbrev.json").getInputStream()) {
            bookByAbbrev = objectMapper.readValue(abbrevStream, new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
            log.error("책 약어 표를 읽지 못했습니다. 참조 조회에서 약어를 쓸 수 없습니다 - {}", e.getMessage());
        }

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:bible/**/*.json");

        for (Resource resource : resources){
            try (InputStream is = resource.getInputStream()){
                List<Logos> chapterVerses = objectMapper.readValue(is, new TypeReference<>() {});

                if (chapterVerses == null || chapterVerses.isEmpty()) {
                    continue;
                }

                List<Logos> validVerses = chapterVerses.stream()
                        .filter(this::isValidVerse)
                        .collect(Collectors.toList());

                if (validVerses.isEmpty()) {
                    log.error("유효한 절이 하나도 없어 파일을 건너뜁니다: {}", resource.getFilename());
                    continue;
                }

                Logos firstVerse = validVerses.get(0);
                String bookName = firstVerse.getBook();
                int chapterNum = Integer.parseInt(firstVerse.getChapter());

                bibleMap.computeIfAbsent(bookName, k -> new HashMap<>())
                        .put(chapterNum, validVerses);
                allVerses.addAll(validVerses);
            }catch (Exception e){
                log.error("파일 로드 실패: {} - {}", resource.getFilename(), e.getMessage());
            }
        }
        totalVerseCount = allVerses.stream().mapToInt(v -> verseSpan(v.getVerse())).sum();
    }

    /**
     * 절 데이터가 book/chapter/verse 값을 모두 정상적으로 갖추고 있는지 검증합니다.
     * chapter/verse는 요청 처리 시 숫자로 파싱되므로, 여기서 미리 걸러내면 잘못된 데이터 하나 때문에
     * 해당 장 전체 조회가 요청 시점에 500 에러로 이어지는 것을 막을 수 있습니다.
     * 형식이 깨진 절만 제외되고, 같은 파일 내 나머지 정상 절은 그대로 서비스됩니다.
     *
     * @param logos 검증할 절 데이터
     * @return 형식이 정상이면 true
     */
    private boolean isValidVerse(Logos logos) {
        if (logos.getBook() == null || logos.getBook().isBlank()) {
            return false;
        }
        if (logos.getChapter() == null || logos.getVerse() == null) {
            return false;
        }
        try {
            Integer.parseInt(logos.getChapter());
            if (!VERSE_PATTERN.matcher(logos.getVerse()).matches()) {
                throw new NumberFormatException(logos.getVerse());
            }
        } catch (NumberFormatException e) {
            log.warn("잘못된 장/절 번호 형식이라 제외함 - book: {}, chapter: {}, verse: {}",
                    logos.getBook(), logos.getChapter(), logos.getVerse());
            return false;
        }
        return true;
    }

    /**
     * 메모리에 적재된 성경 데이터 중 무작위로 하나의 구절을 선택하여 반환합니다.
     * 전체 구절을 flat list로 관리하여 모든 구절이 동일한 확률로 선택됩니다.
     *
     * @return 랜덤하게 선택된 성경 구절 DTO
     */
    public LogosResponseDTO getRandomVerse(){
        if (allVerses.isEmpty()) {
            return LogosResponseDTO.builder()
                    .book("error")
                    .text("성경 데이터를 불러오지 못했습니다.")
                    .build();
        }
        Logos randomLogos = allVerses.get(random.nextInt(allVerses.size()));

        return LogosResponseDTO.from(randomLogos);
    }

    /**
     * 날짜를 시드로 사용해 매일 자정 기준으로 고정되는 구절을 반환합니다.
     * 별도 DB 저장이나 스케줄러 없이, 같은 날짜에는 항상 같은 구절이 계산됩니다.
     *
     * @return 오늘의 말씀 DTO
     */
    public LogosResponseDTO getTodayVerse(){
        if (allVerses.isEmpty()) {
            return LogosResponseDTO.builder()
                    .book("error")
                    .text("성경 데이터를 불러오지 못했습니다.")
                    .build();
        }
        long epochDay = LocalDate.now().toEpochDay();
        int index = (int) (epochDay % allVerses.size());
        return LogosResponseDTO.from(allVerses.get(index));
    }

    /**
     * 현재 등록된 전체 구절 수를 반환합니다.
     * "1-2"처럼 합쳐진 절 번호는 범위에 포함된 절 수만큼 센다. 값은 로드 시점에 한 번 계산한다.
     *
     * @return 전체 구절 수
     */
    public int getTotalVerseCount(){
        return totalVerseCount;
    }

    /**
     * 절 번호의 시작 번호를 반환합니다. "1-2"는 1이다.
     *
     * @param verse 절 번호 문자열
     * @return 시작 절 번호
     */
    private static int verseStart(String verse) {
        return Integer.parseInt(verse.split("-")[0]);
    }

    /**
     * 절 번호 하나가 차지하는 절 수를 반환합니다. "1-2"는 2, "3"은 1이다.
     *
     * @param verse 절 번호 문자열
     * @return 절 수
     */
    private static int verseSpan(String verse) {
        String[] parts = verse.split("-");
        return parts.length == 2 ? Integer.parseInt(parts[1]) - Integer.parseInt(parts[0]) + 1 : 1;
    }

    /**
     * 데이터가 존재하는 책 목록을 정경 순서(구약 39권 -> 신약 27권)로 반환합니다.
     *
     * @return 책 이름 목록
     */
    public List<String> getAvailableBooks(){
        List<String> books = new ArrayList<>();
        OLD_TESTAMENT_ORDER.stream().filter(bibleMap::containsKey).forEach(books::add);
        NEW_TESTAMENT_ORDER.stream().filter(bibleMap::containsKey).forEach(books::add);
        return books;
    }

    /**
     * "요3:16", "요한복음 3:16-18", "요3" 같은 참조 표기로 구절을 찾습니다.
     * 약어와 정식 이름을 모두 받고, 절을 지정하지 않으면 장 전체를 돌려줍니다.
     *
     * @param reference 참조 문자열
     * @return 찾은 구절 목록. 형식이 틀렸거나 없는 참조이면 빈 목록
     */
    public List<LogosResponseDTO> findByReference(String reference){
        return BibleReference.parse(reference)
                .map(this::versesOf)
                .orElseGet(List::of);
    }

    /**
     * 참조 표기가 읽을 수 있는 형식인지 확인합니다. 해당 구절이 있는지까지는 보지 않습니다.
     *
     * @param reference 참조 문자열
     * @return 형식이 맞으면 true
     */
    public boolean isReadableReference(String reference){
        return BibleReference.parse(reference).isPresent();
    }

    /**
     * 참조가 가리키는 절을 찾습니다. 책 이름은 정식 이름을 먼저 보고 없으면 약어 표로 해석합니다.
     *
     * @param reference 나눠 담긴 참조
     * @return 참조에 들어가는 절 목록 (없으면 빈 목록)
     */
    private List<LogosResponseDTO> versesOf(BibleReference reference){
        String book = bibleMap.containsKey(reference.getBook())
                ? reference.getBook()
                : bookByAbbrev.get(reference.getBook());

        if (book == null) {
            return List.of();
        }

        return getVerses(book, reference.getChapter()).stream()
                .filter(v -> reference.contains(verseStart(v.getVerse())))
                .map(LogosResponseDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 참조가 가리키는 책의 정식 이름을 돌려줍니다. 웹에서 해당 장으로 보낼 때 씁니다.
     *
     * @param reference 참조 문자열
     * @return 정식 책 이름. 읽을 수 없거나 등록되지 않은 책이면 빈 값
     */
    public Optional<String> resolveBookName(String reference){
        return BibleReference.parse(reference)
                .map(BibleReference::getBook)
                .map(book -> bibleMap.containsKey(book) ? book : bookByAbbrev.get(book))
                .filter(Objects::nonNull);
    }

    /**
     * 지정한 장의 다음 장을 찾습니다. 책의 마지막 장이면 다음 책의 첫 장을 돌려줍니다.
     * 낭독이 책을 넘어가며 이어지려면 책 순서를 알아야 하는데, 그 순서는 서비스가 가지고 있습니다.
     *
     * @param book    현재 책 이름
     * @param chapter 현재 장 번호
     * @return 다음 책 이름과 장 번호. 성경의 마지막 장이면 빈 값
     */
    public Optional<Map.Entry<String, Integer>> nextChapterOf(String book, int chapter){
        List<Integer> chapters = getChapters(book);
        int index = chapters.indexOf(chapter);

        if (index >= 0 && index < chapters.size() - 1) {
            return Optional.of(Map.entry(book, chapters.get(index + 1)));
        }

        List<String> books = getAvailableBooks();
        int bookIndex = books.indexOf(book);
        if (bookIndex < 0 || bookIndex == books.size() - 1) {
            return Optional.empty();
        }

        String nextBook = books.get(bookIndex + 1);
        List<Integer> nextChapters = getChapters(nextBook);
        if (nextChapters.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(Map.entry(nextBook, nextChapters.get(0)));
    }

    /**
     * 데이터가 등록된 책 이름인지 확인합니다.
     *
     * @param book 확인할 책 이름
     * @return 등록된 책이면 true (null이거나 비어 있으면 false)
     */
    public boolean isRegisteredBook(String book){
        return book != null && !book.isBlank() && bibleMap.containsKey(book);
    }

    /**
     * 데이터가 존재하는 책 목록을 "구약"/"신약"으로 그룹핑하여 반환합니다.
     *
     * @return key가 "구약"/"신약"인 LinkedHashMap (순서 보장)
     */
    public Map<String, List<String>> getAvailableBooksByTestament(){
        Map<String, List<String>> result = new LinkedHashMap<>();
        result.put("구약", OLD_TESTAMENT_ORDER.stream().filter(bibleMap::containsKey).collect(Collectors.toList()));
        result.put("신약", NEW_TESTAMENT_ORDER.stream().filter(bibleMap::containsKey).collect(Collectors.toList()));
        return result;
    }

    /**
     * 지정한 책에 존재하는 장 번호 목록을 오름차순으로 반환합니다.
     *
     * @param book 책 이름
     * @return 장 번호 목록 (데이터 없으면 빈 리스트)
     */
    public List<Integer> getChapters(String book){
        Map<Integer, List<Logos>> chapters = bibleMap.get(book);
        if (chapters == null) {
            return List.of();
        }
        return chapters.keySet().stream().sorted().collect(Collectors.toList());
    }

    /**
     * 지정한 책의 특정 장에 속한 전체 절을 절 번호 순으로 반환합니다.
     *
     * @param book    책 이름
     * @param chapter 장 번호
     * @return 절 목록 (데이터 없으면 빈 리스트)
     */
    public List<Logos> getVerses(String book, int chapter){
        Map<Integer, List<Logos>> chapters = bibleMap.get(book);
        if (chapters == null) {
            return List.of();
        }
        List<Logos> verses = chapters.get(chapter);
        if (verses == null) {
            return List.of();
        }
        return verses.stream()
                .sorted(Comparator.comparingInt(v -> verseStart(v.getVerse())))
                .collect(Collectors.toList());
    }

    /**
     * 본문에 검색어가 포함된 구절을 정경 순서로 찾아 페이지 단위로 반환합니다.
     * 전체 구절이 메모리에 있으므로 별도 색인 없이 순차 탐색한다.
     *
     * 책별 건수(books)는 book 필터와 무관하게 전체 기준으로 세고, 건수가 많은 책부터 담는다.
     * 어느 책을 먼저 볼지 고르는 데 쓰기 때문이다.
     *
     * @param query  검색어 (공백은 앞뒤만 제거하고 그대로 비교한다)
     * @param book   특정 책으로 좁힐 때의 책 이름. null이거나 비어 있으면 전체를 찾는다
     * @param limit  한 번에 돌려줄 최대 건수
     * @param offset 건너뛸 건수
     * @return 전체 일치 건수와 해당 페이지의 구절 목록
     */
    public SearchResponseDTO search(String query, String book, int limit, int offset){
        String needle = query.trim();
        boolean allBooks = book == null || book.isBlank();

        List<Logos> matched = allVerses.stream()
                .filter(v -> v.getText() != null && v.getText().contains(needle))
                .sorted(CANONICAL_ORDER)
                .collect(Collectors.toList());

        Map<String, Integer> countByBook = new HashMap<>();
        matched.forEach(v -> countByBook.merge(v.getBook(), 1, Integer::sum));

        Map<String, Integer> booksWithCount = countByBook.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

        List<Logos> hits = allBooks
                ? matched
                : matched.stream().filter(v -> book.equals(v.getBook())).collect(Collectors.toList());

        List<LogosResponseDTO> page = hits.stream()
                .skip(offset)
                .limit(limit)
                .map(LogosResponseDTO::from)
                .collect(Collectors.toList());

        return SearchResponseDTO.builder()
                .books(booksWithCount)
                .query(needle)
                .total(hits.size())
                .offset(offset)
                .limit(limit)
                .results(page)
                .build();
    }

    /**
     * 구약 39권 + 신약 27권의 정경 순서를 책 이름 -> 순서 번호 맵으로 만듭니다.
     *
     * @return 책 이름을 키로 하는 순서 맵
     */
    private static Map<String, Integer> buildBookOrder(){
        Map<String, Integer> order = new HashMap<>();
        List<String> canon = new ArrayList<>(OLD_TESTAMENT_ORDER);
        canon.addAll(NEW_TESTAMENT_ORDER);
        for (int i = 0; i < canon.size(); i++) {
            order.put(canon.get(i), i);
        }
        return order;
    }
}