package org.example.zapros;

import java.util.List;
import java.util.stream.Collectors;

import org.polytech.zapros.bean.Alternative;
import org.polytech.zapros.bean.AlternativeResult;
import org.polytech.zapros.bean.Answer;
import org.polytech.zapros.bean.Assessment;
import org.polytech.zapros.bean.Criteria;
import org.polytech.zapros.bean.QuasiExpert;
import org.polytech.zapros.bean.QuasiExpertQV;

public class DisplayUtils {

    private DisplayUtils() {
        // No org.example.zapros.DisplayUtils instances for you!
    }

    public static void displayData(Data data) {
        System.out.println("Данные успешно загружены!\n");
        displayCriteria(data);
        displayAlternatives(data);
    }

    private static void displayCriteria(Data data) {
        System.out.println("Критерии: ");
        for (Criteria criteria: data.getCriteria()) {
            System.out.printf("%d - %s%n", criteria.getId(), criteria.getName());

            for (Assessment assessment: criteria.getAssessments()) {
                System.out.printf("\t%d: %s - %d\n",
                    assessment.getId(),
                    assessment.getName(),
                    assessment.getRank()
                );
            }

            System.out.println();
        }
    }

    private static void displayAlternatives(Data data) {
        System.out.println("Альтернативы: ");
        for (Alternative alternative: data.getAlternatives()) {
            System.out.println(alternative.getName());

            System.out.println("Характеристики:");
            for (Assessment assessment: alternative.getAssessments()) {
                System.out.printf("\t%s: %s - %d%n",
                    data.getCriteria().get(assessment.getCriteriaId()).getName(),
                    assessment.getName(),
                    assessment.getRank()
                );
            }

            System.out.println();
        }
    }

    public static void displayAlternativesWithRanks(Data data, List<AlternativeResult> alternativeResultList) {
        System.out.println("Ранжирование успешно проведено!");
        System.out.println("Альтернативы, начиная с лучшей: ");
        for (AlternativeResult alternativeResult: alternativeResultList) {
            System.out.println(alternativeResult.getAlternative().getName());

            System.out.println("Ранги оценок:");

            displayAlternativeResult(alternativeResult);
            System.out.printf("Финальный ранг: %d%n", alternativeResult.getFinalRank());

            System.out.println("Характеристики:");
            for (Assessment assessment: alternativeResult.getAlternative().getAssessments()) {
                System.out.printf("\t%s: %s - %d%n",
                    data.getCriteria().get(assessment.getCriteriaId()).getName(),
                    assessment.getName(),
                    assessment.getRank()
                );
            }

            System.out.println();
        }
    }

    private static void displayAlternativeResult(AlternativeResult alternativeResult) {
        if (alternativeResult.getAssessmentsRanks() != null) {
            displayAlternativeQuasiOrderResult(alternativeResult);
        } else {
            displayAlternativeQVResult(alternativeResult);
        }
    }

    private static void displayAlternativeQuasiOrderResult(AlternativeResult alternativeResult) {
        int count = 1;
        for (QuasiExpert qe: alternativeResult.getAssessmentsRanks().keySet()) {
            System.out.printf("\tКвазиэксперт %d:%n", count);
            System.out.print("\t\tОценки: ");
            for (int r: alternativeResult.getAssessmentsRanks().get(qe)) {
                System.out.printf("%d ", r);
            }
            System.out.printf("\n\t\tРанг: %d%n", alternativeResult.getRelativeRanks().get(qe));
            count++;
        }
    }

    private static void displayAlternativeQVResult(AlternativeResult alternativeResult) {
        int count = 1;
        for (QuasiExpertQV qe: alternativeResult.getRelativeQVRanks().keySet()) {
            System.out.printf("\tКвазиэксперт %d:%n", count);
            System.out.printf("\t\tРанг: %d%n", alternativeResult.getRelativeQVRanks().get(qe));
            count++;
        }
    }

    public static void displaySuccessInfo(List<QuasiExpert> qes, List<Criteria> criteriaList) {
        System.out.println("Квазиэксперты успешно построены!");
        System.out.printf("Кол-во квазиэкспертов: %d%n%n", qes.size());

        for (int i = 0; i < qes.size(); i++) {
            System.out.printf("Квазиэксперт %d:%n", i + 1);
            displayQesData(qes.get(i), criteriaList);
        }
    }

    private static void displayQesData(QuasiExpert quasiExpert, List<Criteria> criteriaList) {
        int len = quasiExpert.getMatrix().length;

        for (int i = 0; i < len; i++) {
            for (int j = 0; j < len; j++) {
                System.out.printf("%d ", quasiExpert.getMatrix()[i][j]);
            }
            Assessment assessment = Assessment.getById(i, criteriaList);
            System.out.println("-> " + quasiExpert.getRanks().get(assessment) + " " + quasiExpert.getOrderedRanks().get(assessment));
        }
        System.out.println();
    }

    public static void displayAnswers(List<Answer> answerList) {
        System.out.println("Ответы ЛПР:");
        for (Answer answer: answerList) {
            System.out.println("Answer{" +
                "i=" + answer.getI().getName() +
                ", j=" + answer.getJ().getName() +
                ", answerType=" + answer.getAnswerType() +
                ", answerAuthor=" + answer.getAnswerAuthor() +
                '}'
            );
        }
        System.out.println();
    }

    public static void displayAlternativeOrder(List<AlternativeResult> alternativeResultList) {
        String collect = alternativeResultList.stream()
            .map(x -> x.getAlternative().getName())
            .collect(Collectors.joining(" -> "));
        System.out.println(collect);
        System.out.println();
    }
}
