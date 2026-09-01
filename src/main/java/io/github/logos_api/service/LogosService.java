package io.github.logos_api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.logos_api.dto.LogosResponseDTO;
import io.github.logos_api.model.Logos;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.json.JsonReadFeature;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
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
            "야고보서", "베드로전서", "베드로후서", "요한일서", "요한이서", "요한삼서",
            "유다서", "요한계시록"
    );

    private Map<String, Map<Integer, List<Logos>>> bibleMap = new HashMap<>();
    private List<Logos> allVerses = new ArrayList<>();
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

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:bible/**/*.json");

        for (Resource resource : resources){
            try (InputStream is = resource.getInputStream()){
                List<Logos> chapterVerses = objectMapper.readValue(is, new TypeReference<>() {});

                if (chapterVerses == null || chapterVerses.isEmpty()) {
                    continue;
                }

                Logos firstVerse = chapterVerses.get(0);
                String bookName = firstVerse.getBook();
                int chapterNum = Integer.parseInt(firstVerse.getChapter());

                bibleMap.computeIfAbsent(bookName, k -> new HashMap<>())
                        .put(chapterNum, chapterVerses);
                allVerses.addAll(chapterVerses);
            }catch (Exception e){
                log.error("파일 로드 실패: {} - {}", resource.getFilename(), e.getMessage());
            }
        }
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
     *
     * @return 전체 구절 수
     */
    public int getTotalVerseCount(){
        return allVerses.size();
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
                .sorted(Comparator.comparingInt(v -> Integer.parseInt(v.getVerse())))
                .collect(Collectors.toList());
    }
}