package org.example.zapros;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.polytech.zapros.bean.Alternative;
import org.polytech.zapros.bean.Assessment;
import org.polytech.zapros.bean.Criteria;

import com.google.gson.Gson;

public class Project {
    private List<Criteria> criteriaList;
    private List<Alternative> alternatives;

    private Project(List<Criteria> criteriaList, List<Alternative> alternatives) {
        this.criteriaList = criteriaList;
        this.alternatives = alternatives;
    }

    public static Project of(String pathCriteria, String pathAlternatives) throws IOException {
        Path pathCriteriaA = Paths.get(pathCriteria);
        Path pathAlternativesA = Paths.get(pathAlternatives);

        String jsonCriteria = new String(Files.readAllBytes(pathCriteriaA));
        String jsonAlternatives = new String(Files.readAllBytes(pathAlternativesA));

        Gson gson = new Gson();

        Project project = gson.fromJson(jsonCriteria, Project.class);
        List<Criteria> criteriaList = project.criteriaList;

        Project project1 = gson.fromJson(jsonAlternatives, Project.class);
        List<Alternative> alternativesFULL = project1.alternatives;
        Collections.shuffle(alternativesFULL);
        List<Alternative> alternatives = alternativesFULL.stream().limit(2000).collect(Collectors.toList());

        long criteriaId = 0;
        int orderAssessmentId = 0;

        for (Criteria criteria: criteriaList) {
            criteria.setId(criteriaId);
            criteria.setOrderId((int) criteriaId);

            for (Assessment assessment: criteria.getAssessments()) {
                assessment.setOrderId(orderAssessmentId);
                assessment.setCriteriaId(criteriaId);
                orderAssessmentId++;
            }
            criteriaId++;
        }

        alternatives.forEach(alternative -> {
            List<Assessment> collect = alternative.getAssessments().stream()
                .map(x -> findByName(x.getName(), criteriaList))
                .sorted(Comparator.comparing(Assessment::getOrderId))
                .collect(Collectors.toList());

            alternative.setAssessments(collect);
        });

        return new Project(criteriaList, alternatives);
    }

    private static Assessment findByName(String name, List<Criteria> criteriaList) {
        for (Criteria criteria: criteriaList) {
            for (Assessment assessment: criteria.getAssessments()) {
                if (!assessment.getName().equals(name)) continue;
                return assessment;
            }
        }
        throw new IllegalStateException("!!! no assessment");
    }

    public List<Criteria> getCriteriaList() {
        return criteriaList;
    }

    public List<Alternative> getAlternatives() {
        return alternatives;
    }
}
