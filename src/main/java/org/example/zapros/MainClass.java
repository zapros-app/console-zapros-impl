package org.example.zapros;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import org.polytech.zapros.VdaZaprosFactory;
import org.polytech.zapros.bean.Answer;
import org.polytech.zapros.bean.Answer.AnswerType;
import org.polytech.zapros.bean.AnswerCheckResult;
import org.polytech.zapros.bean.Assessment;
import org.polytech.zapros.bean.BuildingQesCheckResult;
import org.polytech.zapros.bean.Criteria;
import org.polytech.zapros.bean.MethodType;
import org.polytech.zapros.bean.QuasiExpert;
import org.polytech.zapros.bean.QuasiExpertConfig;
import org.polytech.zapros.bean.ReplacedAnswer;
import org.polytech.zapros.bean.alternative.AlternativeRankingResult;
import org.polytech.zapros.service.buildqe.BuildQesService;
import org.polytech.zapros.service.buildqe.BuildQesServiceImpl;
import org.polytech.zapros.service.main.VdaZaprosService;
import org.polytech.zapros.service.qe.QuasiExpertService;
import org.polytech.zapros.service.qe.QuasiExpertServiceImpl;
import org.polytech.zapros.service.qe.QuasiExpertZaprosService;

public class MainClass {
    public static Scanner in;
    private final static String path1 = "src/main/resources/test-10-3/project-10c3a.json";
    private final static String path2 = "src/main/resources/test-10-3/package-10c3a.json";

    public static void main(String[] args) {
//        in = new Scanner(System.in);
        Project project = null;
        try {
            project = Project.of(path1, path2);
        } catch (IOException e) {
            System.out.println("Error! " + e.getMessage());
            System.exit(-1);
        }
//        DisplayUtils.displayProject(project);

//        validateInput("Как будете готовы отвечать на вопросы, введите в консоль 1:", "1");

        MethodType methodTypeOrder = MethodType.ZAPROS_II;
        MethodType methodTypeQV = MethodType.ZAPROS_III;

        VdaZaprosService serviceOrder = VdaZaprosFactory.getService(methodTypeOrder);
        VdaZaprosService serviceQV = VdaZaprosFactory.getService(methodTypeQV);
        QuasiExpertConfig config = VdaZaprosFactory.getConfig(project.getCriteriaList());

//        List<Answer> answerList = askAllQuestions(serviceOrder, project.getCriteriaList());
//        DisplayUtils.displayAnswers(answerList);
//        List<QuasiExpert> qes = buildQes(serviceOrder, config, project.getCriteriaList(), answerList, 0.25);
//        DisplayUtils.displaySuccessInfo(qes, project.getCriteriaList());

        DisplayUtils.displayBotInfo(project);

        List<Answer> answerList = generateAnswers(serviceOrder, project.getCriteriaList());
        List<QuasiExpert> qes = generateQes(config, project.getCriteriaList(), answerList, 0.25);

        int runNumber = 5;
        long time1 = 0;
        long time2 = 0;
        AlternativeRankingResult resultOrder = null;
        AlternativeRankingResult resultQV = null;
        for (int i = 0; i < runNumber; i++) {
            resultOrder = serviceOrder.rankAlternatives(qes, project.getAlternatives(), project.getCriteriaList(), config);
            resultQV = serviceQV.rankAlternatives(qes, project.getAlternatives(), project.getCriteriaList(), config);
            time1 += resultOrder.getNanoTime();
            time2 += resultQV.getNanoTime();
        }

        long avgTime1 = time1 / runNumber;
        long avgTime2 = time2 / runNumber;

        DisplayUtils.displayBaseInfo(resultOrder, avgTime1, methodTypeOrder);
        DisplayUtils.displayBaseInfo(resultQV, avgTime2, methodTypeQV);
//        DisplayUtils.displayAlternativesWithRanks(project, result.getAlternativeResults());
//        DisplayUtils.displayAlternativeOrder(result.getAlternativeResults());

//        validateInput("Введите любую строку для завершения программы:");
//        in.close();
    }

    private static List<QuasiExpert> buildQes(VdaZaprosService service, QuasiExpertConfig config, List<Criteria> criteriaList, List<Answer> answerList, Double threshold) {
        BuildingQesCheckResult checkResult = service.buildQes(answerList, config, criteriaList, threshold);

        while (!checkResult.isOver()) {
            String result = validateInput(getTextForAskAgain(checkResult), "1", "2", "3");
            AnswerType type = parseAnswer(result);

            ReplacedAnswer replacedAnswer = service.replaceAnswer(checkResult, type);
            DisplayUtils.displayAnswers(replacedAnswer.getNewAnswers());
            checkResult = service.buildQes(replacedAnswer.getNewAnswers(), config, criteriaList, threshold);
        }

        return checkResult.getQes();
    }

    private static List<Answer> askAllQuestions(VdaZaprosService service, List<Criteria> criteriaList) {
        AnswerCheckResult checkResult = service.askFirst(criteriaList);

        while (!checkResult.isOver()) {
            String result = validateInput(getTextForAsk(checkResult), "1", "2", "3");
            AnswerType type = parseAnswer(result);

            checkResult = service.addAnswer(checkResult, type);
        }

        return checkResult.getAnswerList();
    }

    private static AnswerType parseAnswer(String input) {
        switch (input) {
            case "1": return AnswerType.BETTER;
            case "2": return AnswerType.WORSE;
            case "3": return AnswerType.EQUAL;
            default: throw new IllegalStateException("result main TODO");
        }
    }

    private static String getTextForAsk(AnswerCheckResult checkResult) {
        Criteria criteriaI = checkResult.getCriteriaList().get(checkResult.getPCriteriaI());
        Criteria criteriaJ = checkResult.getCriteriaList().get(checkResult.getPCriteriaJ());

        Assessment assessmentI = criteriaI.getAssessments().get(checkResult.getPAssessmentI());
        Assessment assessmentJ = criteriaJ.getAssessments().get(checkResult.getPAssessmentJ());

        Assessment bestAssessmentI = criteriaI.getAssessments().get(0);
        Assessment bestAssessmentJ = criteriaJ.getAssessments().get(0);

        String compareCriteria = String.format("Сравниваем критерии %d - %s и %d - %s%n",
            criteriaI.getId(), criteriaI.getName(),
            criteriaJ.getId(), criteriaJ.getName());

        String chooseAlternative = String.format("Что для вас лучше: %s(%d) и %s или %s и %s(%d), если остальные критерии имеют лучшие показатели?",
            assessmentI.getName(), assessmentI.getRank(), bestAssessmentJ.getName(),
            bestAssessmentI.getName(), assessmentJ.getName(), assessmentJ.getRank());

        return compareCriteria + chooseAlternative;
    }

    private static String getTextForAskAgain(BuildingQesCheckResult checkResult) {
        Assessment i = checkResult.getAnswerForReplacing().getI();
        Assessment j = checkResult.getAnswerForReplacing().getJ();
        AnswerType type = checkResult.getAnswerForReplacing().getAnswerType();

        return String.format("Ранее вы говорили, что %s(%d) %s, чем %s(%d). Данный ответ вызывает противоречия. Что для вас лучше сейчас?",
            i.getName(), i.getRank(), type, j.getName(), j.getRank());
    }

    private static String validateInput(String outputText, String... valid) {
        System.out.println(outputText);
        do {
            System.out.print("Input " + (valid.length != 0 ? Arrays.toString(valid) : "") + ": ");
            String ans = in.nextLine();
            if (valid.length == 0 || Arrays.asList(valid).contains(ans)) {
                System.out.println();
                return ans;
            }
        } while (true);
    }

    private static List<Answer> generateAnswers(VdaZaprosService service, List<Criteria> criteriaList) {
        AnswerCheckResult checkResult = service.askFirst(criteriaList);

        Random random = new Random();
        while (!checkResult.isOver()) {
            int temp = random.nextInt(3);
            AnswerType type = parseAnswer(String.valueOf(temp + 1));

            checkResult = service.addAnswer(checkResult, type);
        }

        System.out.println("Кол-во вопросов к ЛПР (боту): " + checkResult.getAnswerList().size());
        return checkResult.getAnswerList();
    }

    private static List<QuasiExpert> generateQes(QuasiExpertConfig config, List<Criteria> criteriaList, List<Answer> answerList, double threshold) {
        BuildQesService buildQesService = new BuildQesServiceImpl();
        List<QuasiExpert> build = buildQesService.build(answerList, criteriaList, config);
        System.out.println("Кол-во квазиэкспертов изначальное: " + build.size());

        QuasiExpert quasiExpert = build.get(0);
        calculateRang(quasiExpert, criteriaList, config);
        return Collections.singletonList(quasiExpert);
    }

    private static void calculateRang(QuasiExpert quasiExpert, List<Criteria> criteriaList, QuasiExpertConfig config) {
        int cur = 0;
        Map<Assessment, Integer> ranks = new HashMap<>();

        for (Criteria criteria: criteriaList) {
            for (Assessment assessment: criteria.getAssessments()) {
                int rank = 1;
                for (int i = 0; i < config.getLen(); i++) {
                    if (quasiExpert.getMatrix()[cur][i] != 1) {
                        rank++;
                    }
                }
                cur++;

                ranks.put(assessment, rank);
            }
        }
        quasiExpert.setRanks(ranks);
    }
}
