package hu.elte.ntb.typeshift.exercise.service;

import hu.elte.ntb.typeshift.ai.model.ContentType;
import hu.elte.ntb.typeshift.ai.model.ExamType;
import hu.elte.ntb.typeshift.ai.model.TaskType;
import hu.elte.ntb.typeshift.exercise.model.CannedExercise;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Immutable-by-code catalog of the eighteen canned station exercises:
 * thirteen sub-exams plus five graduations (ADR-013).
 */
@Service
public class CannedCatalog {

  /** Station names from ADR-013, keyed by stage id. */
  static final Map<Integer, String> STAGE_NAMES = Map.of(
      1, "QWERTZ Core",
      2, "Symbol Sorcerer",
      3, "Numpad & Math",
      4, "Hungarian Accents",
      5, "Real-World Tasks");

  private static final List<CannedExercise> EXERCISES = List.of(
      prose(1, ExamType.SUB_EXAM, "exam-1", "Home row — asdf jklé", null,
          "A kisfiú a ház előtt áll, és a labdát figyeli. Az édesapa a kertben sétál, "
              + "a kislány pedig jókedvűen dalol, a kutya pedig szalad.",
          45),
      prose(1, ExamType.SUB_EXAM, "exam-2", "Top row — qwertzuiopőú", null,
          "A qwertz billentyűzeten a Q, W, E, R, T, Z betűk őrzik a felső sort, "
              + "az ő és az ú pedig különleges ékezetet kapott a magyar kiosztásban.",
          45),
      prose(1, ExamType.SUB_EXAM, "exam-3", "Bottom row — yxcvbnm,-.", null,
          "A zenekar xilofont, csellót és bőgőt hozott a vásárba - a gyönyörű dallamokra "
              + "a közönség tapssal ünnepelte őket, majd csendben hazament.",
          45),
      prose(1, ExamType.GRADUATION, "grad", "3-minute continuous QWERTZ passage", null,
          "Reggel hét órakor nyitott ki a városháza földszinti iktatója, ahol a hivatalos "
              + "iratok sora várta a sorban álló ügyfeleket. Az első ablaknál egy idős asszony "
              + "nyújtotta át a nyugdíjkérelmét, a másodiknál egy fiatal házaspár a "
              + "lakcímváltozást jelentette be, a harmadiknál pedig egy vállalkozó a működési "
              + "engedélyét szerette volna meghosszabbítani. A hivatalnok minden egyes "
              + "dokumentumot gondosan átnézett, ellenőrizte az aláírásokat, a keltezést és a "
              + "mellékletek meglétét, majd a beadványt iktatószámmal látta el. A sor lassan "
              + "haladt, mert a számítógépes rendszer reggelente különösen nehézkesen indult, "
              + "és a nyomtató is kétszer elakadt. Dél körül aztán megérkezett a segítség: a "
              + "szomszédos osztályról egy kolléga ült le a második pult mögé, és onnantól "
              + "kezdve kétszer olyan gyorsan peregtek a kérelmek. A délutáni szünetben a "
              + "dolgozók a folyosói automatánál kávét vettek, és röviden megbeszélték, hogy a "
              + "következő héten áttérnek az új elektronikus ügyintézési felületre. Az "
              + "ügyfelek többsége türelmesen várakozott, néhányan azonban hangosan "
              + "méltatlankodtak a hosszú sor miatt. Zárás előtt fél órával már csak ketten "
              + "maradtak: egy diák, aki a tanulmányi igazolását kérte, és egy nyugdíjas, aki "
              + "az adóügyi papírjait hozta rendezni. A nap végén a vezető összesítette a "
              + "számokat, és megállapította, hogy száztizenhét kérelmet fogadtak be, ami "
              + "rekordnak számít ebben a hónapban.",
          180),
      prose(2, ExamType.SUB_EXAM, "exam-1", "Punctuation & hyphenation", null,
          "A jegyzőkönyv első pontja a következő: a kérelmet a mellékletekkel együtt kell "
              + "benyújtani; hiányos dokumentáció esetén a hivatal nyolc napon belül "
              + "hiánypótlást kér, a határidő - eltérő rendelkezés hiányában - harminc nap.",
          45),
      prose(2, ExamType.SUB_EXAM, "exam-2", "AltGr symbols, brackets & quotes", null,
          "Kedves Ügyfél! A bejelentkezés az e-mail cím (@) és a jelszó megadása után sem "
              + "működik: „Érvénytelen munkamenet” – a rendszer a (401) & (403) hibakódot "
              + "küldi. Kérjük, ellenőrizze a fiók adatait, és válaszoljon erre a levélre.",
          45),
      prose(2, ExamType.GRADUATION, "grad", "Formatted technical support ticket", null,
          "Jegyzetszám: TS-2026-1043 | Állapot: nyitott | Prioritás: magas\n"
              + "Tárgy: Bejelentkezési hiba a partneri portálon\n"
              + "Leírás: A felhasználó három alkalommal próbált belépni; a rendszer minden "
              + "esetben „Érvénytelen hitelesítés” üzenetet adott vissza. Az e-mail cím (@) "
              + "helyes, a jelszó frissítése megtörtént.\n"
              + "Kért intézkedés: a fiók zárolásának feloldása (azonosító: 8842), valamint "
              + "visszajelzés e-mailben.\n"
              + "Ügyintéző: Kovács Anna – 2026. október 31.",
          180),
      math(3, ExamType.SUB_EXAM, "exam-1", "Keypad orientation & decimal tallying",
          "148,50 + 27,25 = ?", "175,75", 45),
      math(3, ExamType.SUB_EXAM, "exam-2", "Ledger arithmetic",
          "1250,40 - 318,75 = ?", "931,65", 45),
      math(3, ExamType.GRADUATION, "grad", "Cashier Rush math evaluation",
          "1240,50 + 318,75 - 96,40 + 512,30 = ?", "1975,15", 180),
      prose(4, ExamType.SUB_EXAM, "exam-1", "Hungarian prose transcribing [civil service record]",
          null,
          "A hivatalos iratokat kérjük a főbejárat melletti iktatóba eljuttatni még a "
              + "mai nap folyamán. Az ügyintézés határideje tizenöt munkanap, kivéve ha "
              + "sürgősségi kérelmet nyújtott be az illetékes osztályvezetőnél...",
          45),
      prose(4, ExamType.SUB_EXAM, "exam-2", "Accented civic notice", null,
          "A polgármesteri hivatal ügyfélszolgálata tájékoztatja a lakosságot, hogy jövő "
              + "héten megváltozik a nyitvatartás. Hétfőn nyolc órától négyig, csütörtökön "
              + "pedig este hatig fogadják az ügyfeleket; szerdán szünetel a személyes "
              + "ügyintézés. Kérjük, a dokumentumokat előzetesen készítsék elő.",
          45),
      prose(4, ExamType.GRADUATION, "grad", "Formal Hungarian request letter", null,
          "Tisztelt Osztályvezető Asszony! Alulírott, Nagy Éva tisztelettel kérem, hogy a "
              + "2026. évi lakhatási támogatás iránti kérelmemet szíveskedjenek felülvizsgálni. "
              + "A csatolt dokumentumok igazolják, hogy a háztartás jövedelme az elmúlt hat "
              + "hónapban a jogszabályban meghatározott küszöb alá csökkent. Kérem, hogy a "
              + "hiánypótlásra nyitva álló határidőt méltányosságból tizenöt nappal "
              + "hosszabbítsák meg. Tisztelettel: Nagy Éva",
          180),
      prose(5, ExamType.SUB_EXAM, "exam-1", "Chat burst", TaskType.CHAT_BURST,
          "Szia Anna! Megjött a csomag? – Igen, délelőtt átvettem a recepción. – Szuper, "
              + "köszönöm! – A számlát beszkenneltem, és elküldtem e-mailben. – Rendben, "
              + "holnap átnézem, és szólok, ha kérdés merül fel. – Oké, addig is szép napot!",
          45),
      prose(5, ExamType.SUB_EXAM, "exam-2", "Email reply", TaskType.EMAIL_REPLY,
          "Kedves Kolléga! Köszönöm a gyors visszajelzést. A jelentkezési lapot átnéztem, "
              + "két adat hiányzik: az adószám és a számlázási cím. Kérlek, pótold péntekig, "
              + "hogy a szerződést jövő héten aláírhassuk. Ha kérdésed van, hívj nyugodtan. "
              + "Üdvözlettel: Kovács Anna",
          45),
      prose(5, ExamType.SUB_EXAM, "exam-3", "Document paragraph", TaskType.DOCUMENT_PARAGRAPH,
          "A szerződés teljesítésének feltétele, hogy a megrendelő a vállalkozási díjat a "
              + "teljesítésigazolás kiállítását követő tizenöt napon belül átutalja a "
              + "megadott bankszámlára. Késedelmes fizetés esetén a vállalkozó a Polgári "
              + "Törvénykönyv szabályai szerinti késedelmi kamatot számít fel.",
          45),
      prose(5, ExamType.SUB_EXAM, "exam-4", "Code comment", TaskType.CODE_COMMENT,
          "// A számlaösszesítő a tételeket beérkezési sorrendben dolgozza fel, majd a\n"
              + "// kedvezményt a végösszegből vonja le. Ha a fizetési határidő lejárt, a "
              + "rendszer\n// értesítést küld a pénzügyi osztálynak, és naplózza az eseményt.",
          45),
      prose(5, ExamType.GRADUATION, "grad", "Three random tasks in sequence", null,
          "Reggeli teendők: először nézd át a beérkező leveleket, majd válaszolj a sürgős "
              + "megkeresésekre. Ebéd előtt készítsd el a heti jelentés vázlatát, és küldd át "
              + "jóváhagyásra a csapatvezetőnek. Délután egyeztess a könyveléssel a nyitott "
              + "számlákról, végül pedig frissítsd a projekt tábláját, hogy mindenki lássa a "
              + "következő lépéseket.",
          180));

  /**
   * Finds one exercise by station and exam key.
   *
   * @param stageId station identifier
   * @param examKey key within the station
   * @return the exercise, or empty for an unknown stage or key
   */
  public Optional<CannedExercise> find(int stageId, String examKey) {
    return EXERCISES.stream()
        .filter(exercise -> exercise.stageId() == stageId)
        .filter(exercise -> exercise.examKey().equals(examKey))
        .findFirst();
  }

  /**
   * Lists the exercises of one station in catalog order.
   *
   * @param stageId station identifier
   * @return the station's exercises, empty for an unknown stage
   */
  public List<CannedExercise> byStage(int stageId) {
    return EXERCISES.stream()
        .filter(exercise -> exercise.stageId() == stageId)
        .toList();
  }

  private static CannedExercise prose(
      int stageId, ExamType examType, String examKey, String label, TaskType taskType,
      String text, int durationSeconds) {
    return new CannedExercise(stageId, examType, examKey, label, ContentType.PROSE, taskType,
        text, text, durationSeconds);
  }

  private static CannedExercise math(
      int stageId, ExamType examType, String examKey, String label,
      String equation, String answer, int durationSeconds) {
    return new CannedExercise(stageId, examType, examKey, label, ContentType.MATH, null,
        equation, answer, durationSeconds);
  }
}
