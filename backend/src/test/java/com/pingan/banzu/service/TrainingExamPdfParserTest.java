package com.pingan.banzu.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pingan.banzu.dto.TrainingExamPdfQuestionDraft;
import com.pingan.banzu.dto.TrainingExamPdfPreviewResponse;
import com.pingan.banzu.dto.TrainingExamQuestionBankChild;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.mock.web.MockMultipartFile;

class TrainingExamPdfParserTest {

  private final TrainingExamPdfParser parser =
      new TrainingExamPdfParser(new FileUploadValidator());

  @Test
  void parsesChineseExamQuestionsAndAnswerSection() {
    String text =
        """
        2019 年考试真题
        一、单项选择题
        1.进入现场前首先应当（ ）。
        A.佩戴防护用品 B.直接作业 C.关闭警示 D.跳过交底
        二、案例分析题
        案例1
        某班组准备进入有限空间。
        根据以上场景，回答下列问题（1题为单选题）：
        1.首先应采取什么措施（ ）。
        A.检测 B.直接进入 C.关闭通风 D.撤走监护 E.拆除警示
        参考答案与解析
        一、单项选择题
        1.参考答案：A
        解析：进入现场前应佩戴防护用品。
        二、案例分析题
        案例1
        1.参考答案：A
        解析：有限空间作业前应先检测。
        """;

    TrainingExamPdfPreviewResponse preview =
        parser.parseExtractedText("sample.pdf", 1, text);

    assertThat(preview.questions()).hasSize(2);
    assertThat(preview.questions().get(0).correctAnswers()).containsExactly("A");
    assertThat(preview.questions().get(0).answerExplanation()).contains("佩戴防护用品");
    assertThat(preview.questions().get(1).children()).hasSize(1);
    assertThat(preview.questions().get(1).children().get(0).options()).hasSize(5);
    assertThat(preview.warnings()).anyMatch(message -> message.contains("保留 A-E"));
  }

  @Test
  void parsesSingleAndMultipleChoiceSectionsWithoutCases() {
    String text =
        """
        安全知识考试真题
        一、单项选择题
        1.进入现场前首先应当（ ）。
        A.佩戴防护用品 B.直接作业 C.关闭警示 D.跳过交底
        二、多项选择题
        1.班前检查包括哪些内容（ ）。
        A.人员状态 B.设备状态 C.作业环境 D.防护用品 E.天气情况
        参考答案与解析
        一、单项选择题
        1.参考答案：A
        解析：进入现场前应佩戴防护用品。
        二、多项选择题
        1.参考答案：ABCD
        解析：人员、设备、环境和防护用品均需检查。
        """;

    TrainingExamPdfPreviewResponse preview =
        parser.parseExtractedText("choices.pdf", 1, text);

    assertThat(preview.questions()).hasSize(2);
    assertThat(preview.questions().get(0).questionType()).isEqualTo("SINGLE_CHOICE");
    assertThat(preview.questions().get(1).questionType()).isEqualTo("MULTIPLE_CHOICE");
    assertThat(preview.questions().get(1).options()).hasSize(5);
    assertThat(preview.questions().get(1).correctAnswers())
        .containsExactly("A", "B", "C", "D");
    assertThat(preview.errors()).isEmpty();
  }

  @Test
  void parsesChineseCaseMarkersDecimalLinesAndInlineSubjectiveAnswers() {
    String text =
        """
        2024年注册安全工程师考试真题（网络版）
        一、单项选择题
        6.某通道宽度如下：
        1.20m 为通道测量值，判断正确选项（ ）。
        A.选项甲 B.选项乙 C.选项丙 D.选项丁
        二、案例分析题
        案例一【背景资料】
        某项目开展安全检查。
        【问题】
        1.正确措施是（ ）。
        A.措施甲 B.措施乙 C.措施丙 D.措施丁 E.措施戊
        案例二【背景资料】
        某项目开展专项治理。
        【问题】
        1.说明第一项措施。
        2.说明第二项措施。
        参考答案与解析
        一、单项选择题
        6.参考答案：C
        解析：通道宽度应按规范核对。
        二、案例分析题
        案例一
        1.参考答案：E
        解析：应选择措施戊。
        案例二
        参考答案：1.第一项措施答案。2.第二项措施答案。
        """;

    TrainingExamPdfPreviewResponse preview =
        parser.parseExtractedText("layout-2024.pdf", 2, text);

    assertThat(preview.questions()).hasSize(3);
    assertThat(preview.questions().get(0).sourceLabel()).isEqualTo("单选题 6");
    assertThat(preview.questions().get(1).children().get(0).options()).hasSize(5);
    assertThat(preview.questions().get(1).children().get(0).correctAnswers())
        .containsExactly("E");
    assertThat(preview.questions().get(2).children())
        .extracting(child -> child.referenceAnswer())
        .containsExactly("第一项措施答案。", "第二项措施答案。");
    assertThat(preview.errors()).isEmpty();
  }

  @Test
  @EnabledIfSystemProperty(named = "exam.pdf.sample", matches = ".+")
  void parsesConfiguredRealExamPdf() throws Exception {
    Path path = Path.of(System.getProperty("exam.pdf.sample"));
    MockMultipartFile file =
        new MockMultipartFile(
            "file", path.getFileName().toString(), "application/pdf", Files.readAllBytes(path));

    TrainingExamPdfPreviewResponse preview = parser.parse(file);

    assertThat(preview.pageCount()).isEqualTo(9);
    assertThat(preview.questions()).hasSize(22);
    assertThat(preview.questions().subList(0, 18))
        .allMatch(question -> "SINGLE_CHOICE".equals(question.questionType()));
    assertThat(preview.questions().get(0).correctAnswers()).containsExactly("A");
    assertThat(preview.questions().get(17).correctAnswers()).containsExactly("C");
    assertThat(preview.questions().get(18).children()).hasSize(5);
    assertThat(preview.questions().get(18).children().get(2).correctAnswers())
        .containsExactly("A", "B", "C", "D");
    assertThat(preview.questions().get(19).children()).hasSize(4);
    assertThat(preview.questions().get(20).children()).hasSize(4);
    assertThat(preview.questions().get(21).children()).hasSize(5);
    assertThat(preview.errors()).isEmpty();
  }

  @Test
  @EnabledIfSystemProperty(named = "exam.pdf.samples", matches = ".+")
  void auditsConfiguredRealExamPdfDirectory() throws Exception {
    Map<String, ExpectedSample> expectedSamples =
        Map.of(
            "2019",
            new ExpectedSample(
                22,
                36,
                "A,D,B,D,B,C,A,C,D,D,B,C,C,A,B,C,B,C",
                "B,D,ABCD,CDE,ABE"),
            "2020",
            new ExpectedSample(
                24,
                38,
                "B,B,C,A,B,C,A,D,C,A,B,A,D,C,D,C,B,C,B,B",
                "C,B,ABCD,AE,BCD"),
            "2021",
            new ExpectedSample(
                24,
                38,
                "B,D,A,A,C,C,D,D,C,C,D,A,C,A,C,B,A,B,C,B",
                "C,B,ABCD,CE,ACDE"),
            "2022",
            new ExpectedSample(
                24,
                38,
                "D,C,B,D,C,A,D,D,B,B,D,C,B,B,D,A,A,C,A,C",
                "E,B,ABCE,BCDE,ABDE"),
            "2023",
            new ExpectedSample(
                24,
                38,
                "B,D,D,A,A,C,D,B,B,D,D,D,C,B,A,C,A,C,B,C",
                "B,C,ADE,CDE,DE"),
            "2024",
            new ExpectedSample(
                23,
                37,
                "D,A,A,C,B,C,D,A,B,A,C,B,B,C,B,A,C,D,C",
                "A,CDE,ABC,ACDE,E"));
    Path directory = Path.of(System.getProperty("exam.pdf.samples"));
    try (Stream<Path> paths = Files.list(directory)) {
      for (Path path :
          paths
              .filter(candidate -> candidate.getFileName().toString().toLowerCase().endsWith(".pdf"))
              .sorted(Comparator.comparing(candidate -> candidate.getFileName().toString()))
              .toList()) {
        MockMultipartFile file =
            new MockMultipartFile(
                "file", path.getFileName().toString(), "application/pdf", Files.readAllBytes(path));
        TrainingExamPdfPreviewResponse preview = parser.parse(file);
        String year = path.getFileName().toString().substring(0, 4);
        ExpectedSample expected = expectedSamples.get(year);
        assertThat(expected).as("年份 %s 必须配置独立核对基准", year).isNotNull();
        List<TrainingExamPdfQuestionDraft> standalone =
            preview.questions().stream()
                .filter(question -> question.children().isEmpty())
                .toList();
        List<TrainingExamPdfQuestionDraft> cases =
            preview.questions().stream()
                .filter(question -> !question.children().isEmpty())
                .toList();
        String childCounts =
            cases.stream()
                .map(question -> Integer.toString(question.children().size()))
                .reduce((left, right) -> left + "/" + right)
                .orElse("-");
        int answerable =
            preview.questions().stream()
                .mapToInt(question -> question.children().isEmpty() ? 1 : question.children().size())
                .sum();
        assertThat(preview.questions()).hasSize(expected.records());
        assertThat(answerable).isEqualTo(expected.answerable());
        assertThat(cases).extracting(question -> question.children().size())
            .containsExactly(5, 4, 4, 5);
        assertThat(draftAnswerCsv(standalone)).isEqualTo(expected.standaloneAnswers());
        assertThat(childAnswerCsv(cases.get(0).children())).isEqualTo(expected.caseOneAnswers());
        assertThat(preview.errors()).isEmpty();
        assertThat(standalone)
            .allSatisfy(
                question -> {
                  assertThat(question.questionText()).isNotBlank();
                  assertThat(question.options()).hasSize(4);
                  assertThat(question.answerExplanation()).isNotBlank();
                });
        assertThat(cases).allSatisfy(question -> assertThat(question.caseMaterial()).isNotBlank());
        assertThat(cases.get(0).children())
            .allSatisfy(
                child -> {
                  assertThat(child.options()).hasSize(5);
                  assertThat(child.answerExplanation()).isNotBlank();
                  assertThat(child.options().stream().map(option -> option.key()).toList())
                      .containsAll(child.correctAnswers());
                });
        assertThat(cases.subList(1, cases.size()))
            .flatExtracting(question -> question.children())
            .allSatisfy(child -> assertThat(child.referenceAnswer()).isNotBlank());
        System.out.printf(
            "PDF_AUDIT|%s|pages=%d|records=%d|answerable=%d|cases=%s|warnings=%d|errors=%d%n",
            path.getFileName(),
            preview.pageCount(),
            preview.questions().size(),
            answerable,
            childCounts,
            preview.warnings().size(),
            preview.errors().size());
        preview.errors().forEach(error -> System.out.println("PDF_ERROR|" + path.getFileName() + "|" + error));
      }
    }
  }

  private String draftAnswerCsv(List<TrainingExamPdfQuestionDraft> questions) {
    return questions.stream()
        .map(question -> String.join("", question.correctAnswers()))
        .reduce((left, right) -> left + "," + right)
        .orElse("");
  }

  private String childAnswerCsv(List<TrainingExamQuestionBankChild> questions) {
    return questions.stream()
        .map(question -> String.join("", question.correctAnswers()))
        .reduce((left, right) -> left + "," + right)
        .orElse("");
  }

  private record ExpectedSample(
      int records, int answerable, String standaloneAnswers, String caseOneAnswers) {}
}
