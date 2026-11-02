package hu.elte.ntb.typeshift.exercise.service;

import hu.elte.ntb.typeshift.ai.model.ContentType;
import hu.elte.ntb.typeshift.ai.model.ExamType;
import hu.elte.ntb.typeshift.ai.model.TaskType;
import hu.elte.ntb.typeshift.config.AuthProperties;
import hu.elte.ntb.typeshift.exercise.model.CannedExercise;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Signs generated exercise details so scoring can verify them without storing exercise text. */
@Service
public class ExerciseTicketService {

  private static final Duration TICKET_LIFETIME = Duration.ofHours(2);
  private final String secret;

  public ExerciseTicketService(AuthProperties properties) {
    this.secret = properties.jwtSecret();
  }

  public String issue(String username, CannedExercise exercise) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(username)
        .id(UUID.randomUUID().toString())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(TICKET_LIFETIME)))
        .claim("stageId", exercise.stageId())
        .claim("examType", exercise.examType().name())
        .claim("examKey", exercise.examKey())
        .claim("label", exercise.label())
        .claim("contentType", exercise.contentType().name())
        .claim("taskType", exercise.taskType() == null ? "" : exercise.taskType().name())
        .claim("displayText", exercise.displayText())
        .claim("expectedAnswer", exercise.expectedAnswer())
        .claim("durationSeconds", exercise.estimatedDurationSeconds())
        .signWith(signingKey(secret), Jwts.SIG.HS256)
        .compact();
  }

  public CannedExercise verify(String token, String username, int stageId, String examKey) {
    Claims claims =
        Jwts.parser().verifyWith(signingKey(secret)).build().parseSignedClaims(token).getPayload();
    if (!username.equals(claims.getSubject())
        || numberClaim(claims, "stageId") != stageId
        || !examKey.equals(claims.get("examKey", String.class))) {
      throw new IllegalArgumentException("Exercise ticket does not match this submission");
    }
    String taskName = claims.get("taskType", String.class);
    TaskType taskType = taskName.isBlank() ? null : TaskType.valueOf(taskName);
    return new CannedExercise(
        stageId,
        ExamType.valueOf(claims.get("examType", String.class)),
        examKey,
        claims.get("label", String.class),
        ContentType.valueOf(claims.get("contentType", String.class)),
        taskType,
        claims.get("displayText", String.class),
        claims.get("expectedAnswer", String.class),
        numberClaim(claims, "durationSeconds"));
  }

  private static int numberClaim(Claims claims, String name) {
    Object value = claims.get(name);
    if (value instanceof Number number) {
      return number.intValue();
    }
    throw new IllegalArgumentException("Invalid exercise ticket");
  }

  private static SecretKey signingKey(String secret) {
    try {
      byte[] derived =
          MessageDigest.getInstance("SHA-256")
              .digest(("typeshift:exercise-ticket:" + secret).getBytes(StandardCharsets.UTF_8));
      return Keys.hmacShaKeyFor(derived);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is unavailable", e);
    }
  }
}
