package org.example.zapros;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.polytech.zapros.bean.Alternative;
import org.polytech.zapros.bean.Answer;
import org.polytech.zapros.bean.Assessment;
import org.polytech.zapros.bean.Criteria;
import org.polytech.zapros.bean.MethodType;
import org.polytech.zapros.bean.QuasiExpert;
import org.polytech.zapros.bean.QuasiExpertQV;
import org.polytech.zapros.bean.alternative.AlternativeOrderResult;
import org.polytech.zapros.bean.alternative.AlternativeQVResult;
import org.polytech.zapros.bean.alternative.AlternativeRankingResult;
import org.polytech.zapros.bean.alternative.AlternativeResult;
import org.polytech.zapros.bean.alternative.CompareType;

public class DisplayUtils {

    private DisplayUtils() {
        // No org.example.zapros.DisplayUtils instances for you!
    }

    public static void displayProject(Project project) {
        System.out.println("Данные успешно загружены!\n");
        displayCriteria(project);
        displayAlternatives(project);
    }

    private static void displayCriteria(Project project) {
        System.out.println("Критерии: ");
        for (Criteria criteria: project.getCriteriaList()) {
            System.out.printf("%d - %s%n", criteria.getId(), criteria.getName());

            for (Assessment assessment: criteria.getAssessments()) {
                System.out.printf("\t%d: %s - %d\n",
                    assessment.getOrderId(),
                    assessment.getName(),
                    assessment.getRank()
                );
            }

            System.out.println();
        }
    }

    private static void displayAlternatives(Project project) {
        System.out.println("Альтернативы: ");
        for (Alternative alternative: project.getAlternatives()) {
            System.out.println(alternative.getName());

            System.out.println("Характеристики:");
            for (Assessment assessment: alternative.getAssessments()) {
                System.out.printf("\t%s: %s - %d%n",
                    project.getCriteriaList().get((int) assessment.getCriteriaId()).getName(),
                    assessment.getName(),
                    assessment.getRank()
                );
            }

            System.out.println();
        }
    }

    public static void displayAlternativesWithRanks(Project project, List<? extends AlternativeResult> alternativeResultList) {
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
                    project.getCriteriaList().get((int) assessment.getCriteriaId()).getName(),
                    assessment.getName(),
                    assessment.getRank()
                );
            }

            System.out.println();
        }
    }

    private static void displayAlternativeResult(AlternativeResult alternativeResult) {
        if (alternativeResult instanceof AlternativeOrderResult) {
            displayAlternativeQuasiOrderResult((AlternativeOrderResult) alternativeResult);
        } else if (alternativeResult instanceof AlternativeQVResult) {
            displayAlternativeQVResult((AlternativeQVResult) alternativeResult);
        }
    }

    private static void displayAlternativeQuasiOrderResult(AlternativeOrderResult alternativeResult) {
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

    private static void displayAlternativeQVResult(AlternativeQVResult alternativeResult) {
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

    public static void displayAlternativeOrder(List<? extends AlternativeResult> alternativeResultList) {
        String collect = alternativeResultList.stream()
            .map(x -> x.getAlternative().getName())
            .collect(Collectors.joining(" -> "));
        System.out.println(collect);
        System.out.println();
    }

    public static void displayBaseInfo(AlternativeRankingResult result, MethodType methodType) {
        System.out.println(methodType);
        System.out.println("Время: " + result.getNanoTime() + " нс");

        List<Integer> equalCount = new ArrayList<>();
        List<Integer> notComparableCount = new ArrayList<>();
        result.getMapCompare().forEach((qe, map) -> {
            final int[] countCompareEqual = {0};
            final int[] countCompareNotComparable = {0};

            map.forEach((pair, type) -> {
                switch (type) {
                    case EQUAL: countCompareEqual[0]++; break;
                    case NOT_COMPARABLE: countCompareNotComparable[0]++; break;
                }
            });

            equalCount.add(countCompareEqual[0]);
            notComparableCount.add(countCompareNotComparable[0]);
        });
        System.out.println("Кол-во равных: " + equalCount.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        System.out.println("Кол-во несравнимых: " + notComparableCount.stream().map(String::valueOf).collect(Collectors.joining(", ")));

        AlternativeResult alternativeResult = result.getAlternativeResults().get(0);
        int qeCount = -1;
        if (alternativeResult instanceof AlternativeOrderResult) {
            qeCount = ((AlternativeOrderResult) alternativeResult).getRelativeRanks().keySet().size();
        } else if (result.getAlternativeResults().get(0) instanceof AlternativeQVResult) {
            qeCount = ((AlternativeQVResult) alternativeResult).getRelativeQVRanks().keySet().size();
        }
        System.out.println("Кол-во квазиэкспертов: " + qeCount);
        System.out.println("Кол-во рангов: " + result.getAlternativeResults().stream().max(Comparator.comparing(AlternativeResult::getFinalRank)).get().getFinalRank());
        System.out.println();
    }
}
