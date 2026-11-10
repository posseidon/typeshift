package hu.elte.ntb.typeshift.stagemap.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hu.elte.ntb.typeshift.exercise.service.CannedCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.ai.google.genai.api-key=test-key")
@AutoConfigureMockMvc
class StageMapControllerTest {

  @Autowired MockMvc mvc;
  @Autowired CannedCatalog catalog;

  @Test
  void rootAndStageMapArePublic() throws Exception {
    mvc.perform(get("/").param("lang", "en"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Stage Map")));
    mvc.perform(get("/stage-map").param("lang", "en"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Stage Map")));
  }

  @Test
  void everyCatalogExerciseHasASelectableLink() throws Exception {
    String page = mvc.perform(get("/stage-map").param("lang", "en"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    for (int stageId = 1; stageId <= CannedCatalog.STAGE_NAMES.size(); stageId++) {
      for (var exercise : catalog.byStage(stageId)) {
        org.assertj.core.api.Assertions.assertThat(page)
            .contains("href=\"/exercise/" + stageId + "/" + exercise.examKey() + "\"");
      }
    }
    org.assertj.core.api.Assertions.assertThat(page)
        .contains(        "QWERTZ Core", "Symbol Sorcerer", "Numpad &amp; Math", "Hungarian Accents",
        "Real-World Tasks", "stn-task-grid",
        "Slack &amp; Teams quick short answers under 40 words.", "Enter Stage 1")
        .doesNotContain("Locked", "Certified", "Profile");
    org.assertj.core.api.Assertions.assertThat(page)
        .contains("<h2 class=\"headline-md\">QWERTZ Core</h2>");
  }
}
