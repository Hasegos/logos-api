package io.github.logos_api.service;

import io.github.logos_api.common.exception.ResourceNotFoundException;
import io.github.logos_api.model.Logos;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 장 페이지를 공유할 때 쓸 이미지를 그려 주는 서비스입니다.
 * <p>
 * 공유 카드에 모든 장이 같은 그림으로 뜨면 무엇을 나눴는지 알 수 없으므로,
 * 책 이름과 장 번호, 첫 구절을 넣은 그림을 장마다 따로 만듭니다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OgImageService {

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 630;
    private static final int PADDING = 90;
    private static final int MAX_BODY_LINES = 4;
    private static final int CACHE_SIZE = 200;

    private static final Color BACKGROUND = new Color(0xFA, 0xF9, 0xFC);
    private static final Color CARD = Color.WHITE;
    private static final Color BORDER = new Color(0xE4, 0xDF, 0xEE);
    private static final Color VIOLET = new Color(0x67, 0x3A, 0xB7);
    private static final Color INK = new Color(0x21, 0x1A, 0x2E);
    private static final Color INK_SOFT = new Color(0x5B, 0x54, 0x68);

    private final LogosService logosService;

    private final Map<String, byte[]> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                    return size() > CACHE_SIZE;
                }
            });

    private Font titleFont;
    private Font bodyFont;
    private Font captionFont;

    /**
     * 그림에 쓸 글꼴을 한 번만 읽어 둡니다.
     * <p>
     * 본문에 실제로 쓰인 글자만 남긴 글꼴이라 저장소에 함께 둡니다.
     * 서버에 설치된 글꼴에 기대면 서버가 바뀔 때 글자가 깨진 그림이 나갑니다.
     * </p>
     *
     * @throws IOException 글꼴 파일을 읽지 못한 경우
     */
    @PostConstruct
    public void init() throws IOException {
        try (InputStream stream = new ClassPathResource("fonts/NotoSerifKR-og.ttf").getInputStream()) {
            Font base = Font.createFont(Font.TRUETYPE_FONT, stream);
            titleFont = base.deriveFont(Font.PLAIN, 68f);
            bodyFont = base.deriveFont(Font.PLAIN, 38f);
            captionFont = base.deriveFont(Font.PLAIN, 28f);
        } catch (java.awt.FontFormatException e) {
            throw new IOException("공유 이미지 글꼴을 읽지 못했습니다", e);
        }
    }

    /**
     * 지정한 장의 공유 이미지를 PNG 바이트로 그립니다.
     *
     * @param book    책 이름
     * @param chapter 장 번호
     * @return PNG 바이트
     * @throws ResourceNotFoundException 해당 장이 없는 경우
     * @throws IOException               이미지를 쓰는 중 오류가 난 경우
     */
    public byte[] render(String book, int chapter) throws IOException {
        String key = book + "/" + chapter;
        byte[] cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        List<Logos> verses = logosService.requireVerses(book, chapter);

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            drawBackground(graphics);
            drawTitle(graphics, book + " " + chapter + "장");
            drawBody(graphics, verses);
            drawCaption(graphics);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);

        byte[] png = output.toByteArray();
        cache.put(key, png);
        return png;
    }

    /**
     * 배경과 카드, 위쪽 강조선을 그립니다.
     *
     * @param graphics 그림을 그릴 대상
     */
    private void drawBackground(Graphics2D graphics) {
        graphics.setColor(BACKGROUND);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        graphics.setColor(CARD);
        graphics.fillRoundRect(40, 40, WIDTH - 80, HEIGHT - 80, 24, 24);

        graphics.setColor(BORDER);
        graphics.drawRoundRect(40, 40, WIDTH - 80, HEIGHT - 80, 24, 24);

        graphics.setColor(VIOLET);
        graphics.fillRect(PADDING, 108, 8, 72);
    }

    /**
     * 책 이름과 장 번호를 그립니다.
     *
     * @param graphics 그림을 그릴 대상
     * @param title    예: "창세기 10장"
     */
    private void drawTitle(Graphics2D graphics, String title) {
        graphics.setFont(titleFont);
        graphics.setColor(INK);
        graphics.drawString(title, PADDING + 32, 170);
    }

    /**
     * 장의 앞부분을 {@value #MAX_BODY_LINES}줄까지 채워 그립니다.
     * <p>
     * 첫 절만 넣으면 짧은 절일 때 카드가 비어 보이므로, 자리가 남으면 다음 절을 이어 붙입니다.
     * 줄이 모자라면 마지막 줄 끝에 말줄임표를 붙입니다.
     * </p>
     *
     * @param graphics 그림을 그릴 대상
     * @param verses   장의 절 목록 (절 번호 순)
     */
    private void drawBody(Graphics2D graphics, List<Logos> verses) {
        graphics.setFont(bodyFont);
        graphics.setColor(INK_SOFT);

        List<String> lines = new ArrayList<>();
        boolean hasMore = false;

        for (Logos verse : verses) {
            String text = verse.getText() == null ? "" : verse.getText().strip();
            if (text.isEmpty()) {
                continue;
            }

            lines.addAll(wrap(graphics.getFontMetrics(), text, WIDTH - PADDING * 2));

            if (lines.size() >= MAX_BODY_LINES) {
                hasMore = lines.size() > MAX_BODY_LINES || verses.indexOf(verse) < verses.size() - 1;
                break;
            }
        }

        int y = 280;
        for (int i = 0; i < Math.min(lines.size(), MAX_BODY_LINES); i++) {
            boolean last = i == Math.min(lines.size(), MAX_BODY_LINES) - 1;
            graphics.drawString(last && hasMore ? lines.get(i) + "…" : lines.get(i), PADDING, y);
            y += 58;
        }
    }

    /**
     * 아래쪽에 서비스 이름과 번역본을 그립니다.
     *
     * @param graphics 그림을 그릴 대상
     */
    private void drawCaption(Graphics2D graphics) {
        graphics.setFont(captionFont);
        graphics.setColor(VIOLET);
        graphics.drawString("logos-api.com", PADDING, HEIGHT - 96);

        graphics.setColor(INK_SOFT);
        FontMetrics metrics = graphics.getFontMetrics();
        String note = "개역개정";
        graphics.drawString(note, WIDTH - PADDING - metrics.stringWidth(note), HEIGHT - 96);
    }

    /**
     * 주어진 너비에 맞게 글을 줄로 나눕니다.
     * 한글은 띄어쓰기 없이도 이어지므로 글자 단위로 재어 넘깁니다.
     *
     * @param metrics  글자 너비를 재는 데 쓸 정보
     * @param text     나눌 글
     * @param maxWidth 한 줄의 최대 너비
     * @return 줄 목록
     */
    private List<String> wrap(FontMetrics metrics, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();

        for (char character : text.toCharArray()) {
            line.append(character);
            if (metrics.stringWidth(line.toString()) > maxWidth) {
                line.deleteCharAt(line.length() - 1);
                lines.add(line.toString());
                line = new StringBuilder().append(character);
            }
        }

        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }
}
