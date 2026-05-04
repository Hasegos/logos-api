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
import java.util.*;

/**
 * 성경 구절 데이터를 로드하고 관리하며 랜덤 구절을 제공하는 서비스 클래스입니다.
 * 애플리케이션 시작 시 JSON 파일들을 메모리에 캐싱하여 빠른 접근을 보장합니다.
 */
@Slf4j
@Service
public class LogosService {

    private Map<String, Map<Integer, List<Logos>>> bibleMap = new HashMap<>();
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
            }catch (Exception e){
                log.info("파일 로드 실패 : " + resource.getFilename() + " - " + e.getMessage());
            }
        }
    }

    /**
     * 메모리에 적재된 성경 데이터 중 무작위로 하나의 구절을 선택하여 반환합니다.
     * [권 선택 -> 장 선택 -> 절 선택]의 3단계 랜덤 로직을 수행합니다.
     *
     * @return 랜덤하게 선택된 성경 구절 DTO
     */
    public LogosResponseDTO getRandomVerse(){
        if (bibleMap.isEmpty()) {
            return LogosResponseDTO.builder()
                    .book("error")
                    .text("성경 데이터를 불러오지 못했습니다.")
                    .build();
        }

        List<String> bookNames = new ArrayList<>(bibleMap.keySet());
        String selectedBookName = bookNames.get(random.nextInt(bookNames.size()));
        Map<Integer, List<Logos>> chaptersMap = bibleMap.get(selectedBookName);

        List<Integer> chapterNumbers = new ArrayList<>(chaptersMap.keySet());
        Integer selectedChapterNum = chapterNumbers.get(random.nextInt(chapterNumbers.size()));
        List<Logos> verses = chaptersMap.get(selectedChapterNum);

        Logos randomLogos = verses.get(random.nextInt(verses.size()));

        return LogosResponseDTO.from(randomLogos);
    }
}