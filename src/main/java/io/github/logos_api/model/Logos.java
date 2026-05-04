package io.github.logos_api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 성경 구절의 데이터를 담는 도메인 모델 클래스입니다.
 * JSON 파일에서 읽어온 원본 데이터를 객체화하는 데 사용됩니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Logos {

   private String book;
   private String chapter;
   private String verse;
   private String text;
}