package hu.elte.ntb.typeshift.config;

import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

/** Locale selection for the Vietnamese-default interface. */
@Configuration
public class LocaleConfig implements WebMvcConfigurer {

  /**
   * Resolves the locale stored in the user's session, defaulting to Vietnamese.
   *
   * @return the configured locale resolver
   */
  @Bean
  public LocaleResolver localeResolver() {
    SessionLocaleResolver resolver = new SessionLocaleResolver();
    resolver.setDefaultLocale(Locale.forLanguageTag("vi"));
    return resolver;
  }

  /**
   * Accepts locale changes through the {@code lang} request parameter.
   *
   * @return the request interceptor
   */
  @Bean
  public LocaleChangeInterceptor localeChangeInterceptor() {
    LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
    interceptor.setParamName("lang");
    return interceptor;
  }

  /**
   * Registers the locale-change interceptor for MVC requests.
   *
   * @param registry the MVC interceptor registry
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(localeChangeInterceptor());
  }
}
