package com.pingan.banzu.service;

import com.pingan.banzu.common.BusinessException;
import com.pingan.banzu.common.TrainingExamQuestionType;
import com.pingan.banzu.dto.TrainingExamPdfPreviewResponse;
import com.pingan.banzu.dto.TrainingExamPdfQuestionDraft;
import com.pingan.banzu.dto.TrainingExamQuestionBankChild;
import com.pingan.banzu.dto.TrainingExamQuestionOption;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TrainingExamPdfParser {

  private static final int MAX_PAGES = 100;
  private static final int MAX_CHARACTERS = 500_000;
  private static final BigDecimal DEFAULT_SCORE = BigDecimal.ONE.setScale(1);
  private static final Pattern CASE_MARKER =
      Pattern.compile(
          "(?m)^【?案例\\s*([0-9一二三四五六七八九十]+)】?(?:\\s*【背景资料】)?\\s*$");
  private static final Pattern QUESTION_MARKER =
      Pattern.compile("(?m)^(\\d{1,2})[.．、](?!\\d)\\s*");
  private static final Pattern OPTION_MARKER =
      Pattern.compile("(?m)(?:^|(?<=\\s))([A-E])[.．、]\\s*");
  private static final Pattern ANSWER_MARKER =
      Pattern.compile("(?m)^(\\d{1,2})[.．、]\\s*参考答案[:：]\\s*([A-E]+)");
  private static final Pattern SUBJECTIVE_ANSWER_MARKER =
      Pattern.compile(
          "(?m)(?:^\\s*(?:参考答案[:：]\\s*)?|(?<=[。；;])\\s*)(\\d{1,2})[.．、](?!\\d)\\s*");
  private static final Pattern CASE_PROMPT =
      Pattern.compile("(?:根据以上场景[，,]\\s*回答下列问题[^：:]*[：:]|【问题】)");

  private final FileUploadValidator fileUploadValidator;

  public TrainingExamPdfParser(FileUploadValidator fileUploadValidator) {
    this.fileUploadValidator = fileUploadValidator;
  }

  public TrainingExamPdfPreviewResponse parse(MultipartFile file) {
    fileUploadValidator.validateAttachment("PDF", file);
    try (PDDocument document = Loader.loadPDF(file.getBytes())) {
      if (document.getNumberOfPages() > MAX_PAGES) {
        throw new BusinessException("PDF 页数不能超过 " + MAX_PAGES + " 页");
      }
      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setSortByPosition(true);
      String text = normalizeExtractedText(stripper.getText(document));
      return parseExtractedText(
          safeFilename(file.getOriginalFilename()), document.getNumberOfPages(), text);
    } catch (BusinessException exception) {
      throw exception;
    } catch (IOException exception) {
      throw new BusinessException("PDF 解析失败，请确认文件未加密且内容完整");
    }
  }

  TrainingExamPdfPreviewResponse parseExtractedText(
      String filename, int pageCount, String extractedText) {
    String text = normalizeExtractedText(extractedText);
    if (text.length() < 100) {
      throw new BusinessException("PDF 未检测到足够的可复制文本，请先进行 OCR 或转换");
    }
    if (text.length() > MAX_CHARACTERS) {
      throw new BusinessException("PDF 提取文本不能超过 " + MAX_CHARACTERS + " 个字符");
    }
    int answerStart = text.indexOf("参考答案与解析");
    if (answerStart < 0) {
      throw new BusinessException("未识别到“参考答案与解析”章节，请使用带答案的试卷或人工录入");
    }
    String paper = text.substring(0, answerStart);
    String answers = text.substring(answerStart + "参考答案与解析".length());
    List<String> warnings = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    List<TrainingExamPdfQuestionDraft> questions =
        parseChineseExamPaper(paper, answers, warnings, errors);
    if (questions.isEmpty()) {
      throw new BusinessException("未从 PDF 中识别到可预览的试题");
    }
    warnings.add(0, "PDF 未标注分值，已按每道可作答题 1 分生成草稿，请在确认入库前核对");
    int answerableCount =
        questions.stream()
            .mapToInt(question -> question.children().isEmpty() ? 1 : question.children().size())
            .sum();
    warnings.add("共解析 " + questions.size() + " 条题库记录、" + answerableCount + " 道可作答题");
    return new TrainingExamPdfPreviewResponse(
        UUID.randomUUID().toString(),
        filename,
        pageCount,
        text.length(),
        questions,
        List.copyOf(warnings),
        List.copyOf(errors));
  }

  private List<TrainingExamPdfQuestionDraft> parseChineseExamPaper(
      String paper, String answers, List<String> warnings, List<String> errors) {
    int multipleSection =
        indexOfAny(paper, "二、多项选择题", "二、多选题", "多项选择题");
    int caseSection =
        indexOfAny(paper, "二、案例分析题", "三、案例分析题", "案例分析题");
    int singleEnd = firstSectionEnd(paper.length(), multipleSection, caseSection);
    String singlePaper = paper.substring(0, singleEnd);
    String multiplePaper =
        multipleSection < 0
            ? ""
            : paper.substring(
                multipleSection, caseSection > multipleSection ? caseSection : paper.length());
    String casePaper = caseSection < 0 ? "" : paper.substring(caseSection);

    int multipleAnswerSection =
        indexOfAny(answers, "二、多项选择题", "二、多选题", "多项选择题");
    int caseAnswerSection =
        indexOfAny(
            answers, "二、案例分析题", "三、案例分析题", "案例分析题");
    int singleAnswerEnd =
        firstSectionEnd(
            answers.length(), multipleAnswerSection, caseAnswerSection);
    Map<Integer, ParsedAnswer> singleAnswers =
        parseObjectiveAnswers(answers.substring(0, singleAnswerEnd));
    List<TrainingExamPdfQuestionDraft> result =
        new ArrayList<>(
            parseStandaloneChoices(
                singlePaper,
                singleAnswers,
                TrainingExamQuestionType.SINGLE_CHOICE,
                errors));
    if (multipleSection >= 0) {
      int multipleAnswerEnd =
          caseAnswerSection > multipleAnswerSection
              ? caseAnswerSection
              : answers.length();
      String multipleAnswerPaper =
          multipleAnswerSection < 0
              ? ""
              : answers.substring(multipleAnswerSection, multipleAnswerEnd);
      result.addAll(
          parseStandaloneChoices(
              multiplePaper,
              parseObjectiveAnswers(multipleAnswerPaper),
              TrainingExamQuestionType.MULTIPLE_CHOICE,
              errors));
    }
    if (caseSection < 0) {
      return result;
    }
    if (caseAnswerSection < 0) {
      errors.add("答案区未识别到案例分析题章节");
      return result;
    }
    Map<Integer, String> casePapers = splitCases(casePaper);
    Map<Integer, String> caseAnswers = splitCases(answers.substring(caseAnswerSection));
    for (Map.Entry<Integer, String> entry : casePapers.entrySet()) {
      int caseNumber = entry.getKey();
      String answerBlock = caseAnswers.getOrDefault(caseNumber, "");
      TrainingExamPdfQuestionDraft draft =
          parseCase(caseNumber, entry.getValue(), answerBlock, warnings, errors);
      if (draft != null) {
        result.add(draft);
      }
    }
    return result;
  }

  private List<TrainingExamPdfQuestionDraft> parseStandaloneChoices(
      String paper,
      Map<Integer, ParsedAnswer> answers,
      TrainingExamQuestionType type,
      List<String> errors) {
    List<QuestionBlock> blocks = splitQuestionBlocks(paper);
    List<TrainingExamPdfQuestionDraft> questions = new ArrayList<>();
    for (QuestionBlock block : blocks) {
      ParsedChoice choice = parseChoice(block.content());
      ParsedAnswer answer = answers.get(block.number());
      int expected = type == TrainingExamQuestionType.SINGLE_CHOICE ? 4 : 5;
      if (choice == null || choice.options().size() < expected) {
        errors.add(
            type.label()
                + " "
                + block.number()
                + "：未完整识别 "
                + (expected == 4 ? "A-D" : "A-E")
                + " 选项");
        continue;
      }
      if (answer == null
          || (type == TrainingExamQuestionType.SINGLE_CHOICE
              && answer.answers().size() != 1)
          || (type == TrainingExamQuestionType.MULTIPLE_CHOICE
              && answer.answers().size() < 2)) {
        errors.add(type.label() + " " + block.number() + "：参考答案数量不符合题型规则");
        continue;
      }
      questions.add(
          draft(
              type.label() + " " + block.number(),
              type,
              choice.questionText(),
              choice.options().subList(0, expected),
              answer.answers(),
              "",
              answer.explanation(),
              "",
              List.of(),
              DEFAULT_SCORE));
    }
    return questions;
  }

  private int indexOfAny(String value, String... candidates) {
    int result = -1;
    for (String candidate : candidates) {
      int index = value.indexOf(candidate);
      if (index >= 0 && (result < 0 || index < result)) {
        result = index;
      }
    }
    return result;
  }

  private int firstSectionEnd(int fallback, int... sections) {
    int result = fallback;
    for (int section : sections) {
      if (section >= 0 && section < result) {
        result = section;
      }
    }
    return result;
  }

  private TrainingExamPdfQuestionDraft parseCase(
      int caseNumber,
      String paperBlock,
      String answerBlock,
      List<String> warnings,
      List<String> errors) {
    Matcher prompt = CASE_PROMPT.matcher(paperBlock);
    if (!prompt.find()) {
      errors.add("案例 " + caseNumber + "：未识别到案例材料与问题分界");
      return null;
    }
    String material = cleanText(paperBlock.substring(0, prompt.start()));
    List<QuestionBlock> questionBlocks =
        splitQuestionBlocks(paperBlock.substring(prompt.end()));
    if (questionBlocks.isEmpty()) {
      errors.add("案例 " + caseNumber + "：未识别到子题");
      return null;
    }
    List<TrainingExamQuestionBankChild> children =
        caseNumber == 1
            ? parseObjectiveCaseChildren(caseNumber, questionBlocks, answerBlock, warnings, errors)
            : parseSubjectiveCaseChildren(caseNumber, questionBlocks, answerBlock, errors);
    if (children.isEmpty()) {
      errors.add("案例 " + caseNumber + "：没有可导入的子题");
      return null;
    }
    BigDecimal total =
        children.stream()
            .map(TrainingExamQuestionBankChild::score)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(1);
    return draft(
        "案例 " + caseNumber,
        TrainingExamQuestionType.CASE_ANALYSIS,
        "案例" + caseNumber,
        List.of(),
        List.of(),
        "",
        "",
        material,
        children,
        total);
  }

  private List<TrainingExamQuestionBankChild> parseObjectiveCaseChildren(
      int caseNumber,
      List<QuestionBlock> blocks,
      String answerBlock,
      List<String> warnings,
      List<String> errors) {
    Map<Integer, ParsedAnswer> answers = parseObjectiveAnswers(answerBlock);
    List<TrainingExamQuestionBankChild> children = new ArrayList<>();
    for (QuestionBlock block : blocks) {
      ParsedChoice choice = parseChoice(block.content());
      ParsedAnswer answer = answers.get(block.number());
      if (choice == null || answer == null) {
        errors.add("案例 " + caseNumber + " 第 " + block.number() + " 题：题目或答案不完整");
        continue;
      }
      TrainingExamQuestionType type =
          answer.answers().size() == 1
              ? TrainingExamQuestionType.SINGLE_CHOICE
              : TrainingExamQuestionType.MULTIPLE_CHOICE;
      List<TrainingExamQuestionOption> options = choice.options();
      if (type == TrainingExamQuestionType.SINGLE_CHOICE && options.size() > 4) {
        warnings.add(
            "案例 "
                + caseNumber
                + " 第 "
                + block.number()
                + " 题为五选一，已按案例不定项单选题保留 A-E 五个原始选项");
      }
      boolean validOptionCount =
          type == TrainingExamQuestionType.SINGLE_CHOICE
              ? options.size() == 4 || options.size() == 5
              : options.size() == 5;
      if (!validOptionCount) {
        errors.add(
            "案例 "
                + caseNumber
                + " 第 "
                + block.number()
                + " 题："
                + type.label()
                + "选项数量不符合题库规则");
        continue;
      }
      children.add(
          child(
              type,
              choice.questionText(),
              options,
              answer.answers(),
              "",
              answer.explanation()));
    }
    return children;
  }

  private List<TrainingExamQuestionBankChild> parseSubjectiveCaseChildren(
      int caseNumber,
      List<QuestionBlock> blocks,
      String answerBlock,
      List<String> errors) {
    Map<Integer, String> answers = parseSubjectiveAnswers(answerBlock);
    List<TrainingExamQuestionBankChild> children = new ArrayList<>();
    for (QuestionBlock block : blocks) {
      String reference = answers.get(block.number());
      if (reference == null || reference.isBlank()) {
        errors.add("案例 " + caseNumber + " 第 " + block.number() + " 题：未识别参考答案");
        continue;
      }
      children.add(
          child(
              TrainingExamQuestionType.SHORT_ANSWER,
              cleanText(block.content()),
              List.of(),
              List.of(),
              reference,
              ""));
    }
    return children;
  }

  private Map<Integer, ParsedAnswer> parseObjectiveAnswers(String section) {
    Matcher matcher = ANSWER_MARKER.matcher(section);
    List<Match> matches = matches(matcher);
    Map<Integer, ParsedAnswer> answers = new LinkedHashMap<>();
    for (int index = 0; index < matches.size(); index++) {
      Match current = matches.get(index);
      int end = index + 1 < matches.size() ? matches.get(index + 1).start() : section.length();
      String explanation = section.substring(current.end(), end);
      explanation = explanation.replaceFirst("^\\s*解析[:：]\\s*", "");
      answers.put(
          Integer.parseInt(current.group(1)),
          new ParsedAnswer(answerKeys(current.group(2)), cleanText(explanation)));
    }
    return answers;
  }

  private Map<Integer, String> parseSubjectiveAnswers(String section) {
    Matcher matcher = SUBJECTIVE_ANSWER_MARKER.matcher(section);
    List<Match> matches = matches(matcher);
    Map<Integer, String> answers = new LinkedHashMap<>();
    for (int index = 0; index < matches.size(); index++) {
      Match current = matches.get(index);
      int end = index + 1 < matches.size() ? matches.get(index + 1).start() : section.length();
      answers.put(
          Integer.parseInt(current.group(1)),
          cleanText(section.substring(current.end(), end)));
    }
    return answers;
  }

  private Map<Integer, String> splitCases(String section) {
    Matcher matcher = CASE_MARKER.matcher(section);
    List<Match> matches = matches(matcher);
    Map<Integer, String> cases = new LinkedHashMap<>();
    for (int index = 0; index < matches.size(); index++) {
      Match current = matches.get(index);
      int end = index + 1 < matches.size() ? matches.get(index + 1).start() : section.length();
      cases.put(caseNumber(current.group(1)), section.substring(current.end(), end).trim());
    }
    return cases;
  }

  private List<QuestionBlock> splitQuestionBlocks(String section) {
    Matcher matcher = QUESTION_MARKER.matcher(section);
    List<Match> matches = matches(matcher);
    List<QuestionBlock> blocks = new ArrayList<>();
    for (int index = 0; index < matches.size(); index++) {
      Match current = matches.get(index);
      int end = index + 1 < matches.size() ? matches.get(index + 1).start() : section.length();
      blocks.add(
          new QuestionBlock(
              Integer.parseInt(current.group(1)),
              section.substring(current.end(), end).trim()));
    }
    return blocks;
  }

  private ParsedChoice parseChoice(String block) {
    Matcher matcher = OPTION_MARKER.matcher(block);
    List<Match> matches = matches(matcher);
    if (matches.isEmpty()) {
      return null;
    }
    String questionText = cleanText(block.substring(0, matches.get(0).start()));
    List<TrainingExamQuestionOption> options = new ArrayList<>();
    for (int index = 0; index < matches.size(); index++) {
      Match current = matches.get(index);
      int end = index + 1 < matches.size() ? matches.get(index + 1).start() : block.length();
      options.add(
          new TrainingExamQuestionOption(
              current.group(1), cleanText(block.substring(current.end(), end))));
    }
    return new ParsedChoice(questionText, options);
  }

  private TrainingExamQuestionBankChild child(
      TrainingExamQuestionType type,
      String questionText,
      List<TrainingExamQuestionOption> options,
      List<String> answers,
      String referenceAnswer,
      String explanation) {
    return new TrainingExamQuestionBankChild(
        type.name(),
        type.label(),
        questionText,
        List.copyOf(options),
        List.copyOf(answers),
        referenceAnswer,
        explanation,
        DEFAULT_SCORE);
  }

  private TrainingExamPdfQuestionDraft draft(
      String sourceLabel,
      TrainingExamQuestionType type,
      String questionText,
      List<TrainingExamQuestionOption> options,
      List<String> answers,
      String referenceAnswer,
      String explanation,
      String material,
      List<TrainingExamQuestionBankChild> children,
      BigDecimal score) {
    return new TrainingExamPdfQuestionDraft(
        sourceLabel,
        type.name(),
        type.label(),
        questionText,
        List.copyOf(options),
        List.copyOf(answers),
        referenceAnswer,
        explanation,
        material,
        List.copyOf(children),
        score);
  }

  private List<String> answerKeys(String value) {
    return value == null
        ? List.of()
        : value
            .toUpperCase()
            .chars()
            .filter(character -> character >= 'A' && character <= 'E')
            .mapToObj(character -> Character.toString((char) character))
            .distinct()
            .sorted()
            .toList();
  }

  private List<Match> matches(Matcher matcher) {
    List<Match> matches = new ArrayList<>();
    while (matcher.find()) {
      List<String> groups = new ArrayList<>();
      for (int index = 0; index <= matcher.groupCount(); index++) {
        groups.add(matcher.group(index));
      }
      matches.add(new Match(matcher.start(), matcher.end(), groups));
    }
    return matches;
  }

  private String normalizeExtractedText(String value) {
    if (value == null) {
      return "";
    }
    String normalized =
        value
            .replace('\r', '\n')
            .replace('\u00a0', ' ')
            .replace('﹣', '-')
            .replace('－', '-')
            .replace("。\n", "。\n");
    List<String> lines = new ArrayList<>();
    for (String rawLine : normalized.split("\\n+")) {
      String line = rawLine.strip();
      if (line.matches("^2019\\s*-\\s*2023年注册安全工程师考试真题及解析$")
          || line.matches("^2024年注册安全工程师考试真题（网络版）$")
          || line.matches("^-\\s*\\d+\\s*-$")) {
        continue;
      }
      if (!line.isEmpty()) {
        lines.add(line);
      }
    }
    return String.join("\n", lines);
  }

  private String cleanText(String value) {
    return value == null ? "" : value.replaceAll("\\s+", " ").trim();
  }

  private String safeFilename(String filename) {
    return filename == null || filename.isBlank() ? "exam.pdf" : filename.trim();
  }

  private int caseNumber(String value) {
    if (value.chars().allMatch(Character::isDigit)) {
      return Integer.parseInt(value);
    }
    return switch (value) {
      case "一" -> 1;
      case "二" -> 2;
      case "三" -> 3;
      case "四" -> 4;
      case "五" -> 5;
      case "六" -> 6;
      case "七" -> 7;
      case "八" -> 8;
      case "九" -> 9;
      case "十" -> 10;
      default -> throw new BusinessException("无法识别案例编号：" + value);
    };
  }

  private record ParsedAnswer(List<String> answers, String explanation) {}

  private record ParsedChoice(
      String questionText, List<TrainingExamQuestionOption> options) {}

  private record QuestionBlock(int number, String content) {}

  private record Match(int start, int end, List<String> groups) {
    private String group(int index) {
      return groups.get(index);
    }
  }
}
