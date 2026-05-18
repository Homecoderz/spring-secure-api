package ci.homecoderz.springsecureapi.entities.dto;

import java.time.Instant;

public record ErrorDTO(String message, int status, Instant timestamp) {
   public static ErrorDTO of(String message, int status) {
       return new ErrorDTO(message, status, Instant.now());
   }
}
