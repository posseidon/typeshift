package hu.elte.ntb.typeshift.ai.service;

import hu.elte.ntb.typeshift.ai.model.AiRequest;
import hu.elte.ntb.typeshift.ai.model.AiResponse;

/** Provider-agnostic content generation (ADR-004). */
public interface AIContentService {

  /**
   * Generates one exercise for the given request.
   *
   * @param request the generation request
   * @return the generated exercise envelope
   */
  AiResponse generate(AiRequest request);
}
