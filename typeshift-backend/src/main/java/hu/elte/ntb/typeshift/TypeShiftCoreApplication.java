package hu.elte.ntb.typeshift;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * TypeShift entry point: boots the deployable backend serving the shell and REST services.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public final class TypeShiftCoreApplication {

  private TypeShiftCoreApplication() {}

  /**
   * Starts the TypeShift application.
   *
   * @param args command-line arguments forwarded to Spring
   */
  public static void main(String[] args) {
    SpringApplication.run(TypeShiftCoreApplication.class, args);
  }
}
