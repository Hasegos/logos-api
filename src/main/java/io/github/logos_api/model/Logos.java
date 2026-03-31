package io.github.logos_api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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