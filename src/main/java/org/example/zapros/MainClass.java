package org.example.zapros;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.polytech.zapros.QuasiExpertConfigFactory;
import org.polytech.zapros.VdaZaprosFactory;
import org.polytech.zapros.VdaZaprosWrapper;
import org.polytech.zapros.bean.AlternativeResult;
import org.polytech.zapros.bean.Answer;
import org.polytech.zapros.bean.Answer.AnswerType;
import org.polytech.zapros.bean.AnswerCheckResult;
import org.polytech.zapros.bean.Assessment;
import org.polytech.zapros.bean.BuildingQesCheckResult;
import org.polytech.zapros.bean.Criteria;
import org.polytech.zapros.bean.MethodType;
import org.polytech.zapros.bean.QuasiExpert;
import org.polytech.zapros.bean.QuasiExpertConfig;

public class MainClass {
    public static Scanner in;
    private final static String path = "src/main/resources/data_nir.txt";

    public static void main(String[] args) {
        in = new Scanner(System.in);
        Data data = Data.loadData(path);
//        DisplayUtils.displayData(data);

        validateInput("Как будете готовы отвечать на вопросы, введите в консоль 1:", "1");

        VdaZaprosWrapper wrapper = new VdaZaprosWrapper(
            VdaZaprosFactory.getService(MethodType.ARACE_QV),
            QuasiExpertConfigFactory.getConfig(data.getCriteria(), 0.25)
        );

//        List<Answer> answerList = askAllQuestions(wrapper, data.getCriteria());
        List<Answer> answerList = getMyAnswers(data.getCriteria(), false);
        DisplayUtils.displayAnswers(answerList);

        List<QuasiExpert> qes = buildQes(wrapper, answerList);
        DisplayUtils.displaySuccessInfo(qes, data.getCriteria());

        List<AlternativeResult> result = wrapper.getService().rankAlternatives(qes, data.getAlternatives(), wrapper.getConfig());
        DisplayUtils.displayAlternativesWithRanks(data, result);
        DisplayUtils.displayAlternativeOrder(result);

        validateInput("Введите любую строку для завершения программы:");
        in.close();
    }

    private static List<QuasiExpert> buildQes(VdaZaprosWrapper wrapper, List<Answer> answerList) {
        BuildingQesCheckResult checkResult = wrapper.getService().buildQes(answerList, wrapper.getConfig());

        while (!checkResult.isOver()) {
            String result = validateInput(getTextForAskAgain(checkResult), "1", "2", "3");
            AnswerType type = parseAnswer(result);

            List<Answer> newAnswerList = wrapper.getService().replaceAnswer(checkResult, type);
            DisplayUtils.displayAnswers(newAnswerList);
            checkResult = wrapper.getService().buildQes(newAnswerList, wrapper.getConfig());
        }

        return checkResult.getQes();
    }

    private static List<Answer> askAllQuestions(VdaZaprosWrapper wrapper, List<Criteria> criteriaList) {
        AnswerCheckResult checkResult = wrapper.getService().askFirst(criteriaList);
        QuasiExpertConfig config = wrapper.getConfig();

        while (!checkResult.isOver()) {
            String result = validateInput(getTextForAsk(checkResult), "1", "2", "3");
            AnswerType type = parseAnswer(result);

            checkResult = wrapper.getService().addAnswer(checkResult, type, config);
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
        Criteria criteriaI = checkResult.getCriteriaList().get(checkResult.getpCriteriaI());
        Criteria criteriaJ = checkResult.getCriteriaList().get(checkResult.getpCriteriaJ());

        Assessment assessmentI = criteriaI.getAssessments().get(checkResult.getpAssessmentI());
        Assessment assessmentJ = criteriaJ.getAssessments().get(checkResult.getpAssessmentJ());

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

    // TEST
    private static List<Answer> getMyAnswers(List<Criteria> criteriaList, boolean twoQe) {
        List<Answer> result = new ArrayList<>();

//        Ответы ЛПР:
//        Answer{i=A2, j=B2, answerType=BETTER}
//        Answer{i=A3, j=B2, answerType=WORSE}
//        Answer{i=A3, j=B3, answerType=WORSE}
//        Answer{i=A2, j=C2, answerType=WORSE}
//        Answer{i=A2, j=C3, answerType=BETTER}
//        Answer{i=A3, j=C3, twoQe ? answerType=BETTER : answerType=WORSE}
//        Answer{i=B2, j=C2, answerType=WORSE}
//        Answer{i=B2, j=C3, answerType=BETTER}
//        Answer{i=B3, j=C3, answerType=WORSE}

        result.add(new Answer(
            criteriaList.get(0).getAssessments().get(1),
            criteriaList.get(1).getAssessments().get(1),
            AnswerType.BETTER,
            Answer.AnswerAuthor.USER
        ));

        result.add(new Answer(
            criteriaList.get(0).getAssessments().get(2),
            criteriaList.get(1).getAssessments().get(1),
            AnswerType.WORSE,
            Answer.AnswerAuthor.USER
        ));

        result.add(new Answer(
            criteriaList.get(0).getAssessments().get(2),
            criteriaList.get(1).getAssessments().get(2),
            AnswerType.WORSE,
            Answer.AnswerAuthor.USER
        ));
//
//        result.add(new Answer(
//            criteriaList.get(0).getAssessments().get(2),
//            criteriaList.get(1).getAssessments().get(3),
//            AnswerType.BETTER,
//            Answer.AnswerAuthor.USER
//        ));

        result.add(new Answer(
            criteriaList.get(0).getAssessments().get(1),
            criteriaList.get(2).getAssessments().get(1),
            AnswerType.WORSE,
            Answer.AnswerAuthor.USER
        ));

        result.add(new Answer(
            criteriaList.get(0).getAssessments().get(1),
            criteriaList.get(2).getAssessments().get(2),
            AnswerType.BETTER,
            Answer.AnswerAuthor.USER
        ));

        result.add(new Answer(
            criteriaList.get(0).getAssessments().get(2),
            criteriaList.get(2).getAssessments().get(2),
            twoQe ? AnswerType.BETTER : AnswerType.WORSE,
            Answer.AnswerAuthor.USER
        ));

        result.add(new Answer(
            criteriaList.get(1).getAssessments().get(1),
            criteriaList.get(2).getAssessments().get(1),
            AnswerType.WORSE,
            Answer.AnswerAuthor.USER
        ));

        result.add(new Answer(
            criteriaList.get(1).getAssessments().get(1),
            criteriaList.get(2).getAssessments().get(2),
            AnswerType.BETTER,
            Answer.AnswerAuthor.USER
        ));

        result.add(new Answer(
            criteriaList.get(1).getAssessments().get(2),
            criteriaList.get(2).getAssessments().get(2),
            AnswerType.WORSE,
            Answer.AnswerAuthor.USER
        ));

        return result;
    }

}
