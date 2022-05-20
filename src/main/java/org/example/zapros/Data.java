package org.example.zapros;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.zapros.bean.Alternative;
import com.zapros.bean.Assessment;
import com.zapros.bean.Criteria;

/**
 * Данный класс служит для загрузки данных из файла.
 * Написан самописный парсер для обработки файла.
 * <p>
 * Помечен как устаревший, поскольку будет
 * нормальный формат вместо этого (JSON).
 */
@Deprecated
public class Data {
    private final ArrayList<Criteria> criteria;
    private final ArrayList<Alternative> alternatives;
    private boolean isDataOk = true;

    public Data (File file) {
        criteria = new ArrayList<>();
        alternatives = new ArrayList<>();
        try {
            InputStream inputStream = new FileInputStream(file);
            Scanner in = new Scanner(inputStream, System.getProperty("console.encoding", "utf-8"));
            String line;

            while (in.hasNextLine() && isDataOk) {
                line = getLine(in);

                if (line.toLowerCase().contains("criteria")) {
                    criteria.add(readCriteria(in));
                }
                else if (line.toLowerCase().contains("alternative")) {
                    alternatives.add(readAlternative(in));
                }
            }
            in.close();
        } catch (Exception e) {
            isDataOk = false;
        }
        if ((alternatives.size() == 0) || (criteria.size() == 0)) {
            isDataOk = false;
        }

        if (isDataOk) {
            sortingInitData();
            Assessment.calculateId(criteria);
        }
    }

    public static Data loadData(String path) {
        File file = new File(path);

        Data data = new Data(file);
        if (data.isDataNotOk()) {
            System.out.println("Ошибка в исходных данных!");
            System.exit(0);
        }

        return data;
    }

    //region reading input data.txt
    private static String getLine(Scanner in) {
        return in.nextLine().trim();
    }

    private Criteria readCriteria(Scanner in) {
        List<Assessment> assessments = new ArrayList<>();
        String name = getLine(in);
        int id = Integer.parseInt(getLine(in));

        String line;
        while ((line = getLine(in)).toLowerCase().contains("assessment"))
            assessments.add(readAssessment(in, id));

        if (line.equals("}"))
            return new Criteria(name, id, assessments);
        else isDataOk = false;

        return null;
    }

    private Assessment readAssessment(Scanner in, int id) {
        String name = getLine(in);
        int rank = Integer.parseInt(getLine(in));

        if (getLine(in).equals("}"))
            return new Assessment(name, id, rank);
        else isDataOk = false;

        return null;
    }

    private Alternative readAlternative(Scanner in) {
        String name = getLine(in);
        List<Assessment> assessments;

        if (getLine(in).toLowerCase().contains("assessments")) {
            assessments = findAssessments(in);
        } else {
            isDataOk = false;
            return null;
        }

        if ((getLine(in).equals("}")) && (assessments != null))
            return new Alternative(name, assessments);
        else isDataOk = false;

        return null;
    }

    private List<Assessment> findAssessments(Scanner in) {
        List<Assessment> assessments = new ArrayList<>();

        List<Integer> crIdsFromAlternatives = new ArrayList<>();
        List<Integer> crIdsFromCriteria = criteria
                .stream()
                .map(Criteria::getId)
                .collect(Collectors.toCollection(ArrayList::new));

        for (int i = 0; i < criteria.size(); i++) {
            String name = getLine(in);

            for (Criteria c: criteria) {
                for (Assessment a: c.getAssessments()) {
                    if (a.getName().equals(name)) {
                        assessments.add(a);
                        crIdsFromAlternatives.add(a.getCriteriaId());
                        break;
                    }
                }
            }
        }

        crIdsFromCriteria.sort(Comparator.naturalOrder());
        crIdsFromAlternatives.sort(Comparator.naturalOrder());
        if (!crIdsFromCriteria.equals(crIdsFromAlternatives)) {
            isDataOk = false;
            return null;
        }

        if (getLine(in).equals("}"))
            return assessments;
        else isDataOk = false;

        return null;
    }
    //endregion

    private void sortingInitData() {
        criteria.sort(Comparator.comparingInt(Criteria::getId));
        for (Criteria c: criteria) {
            c.getAssessments().sort(Comparator.comparingInt(Assessment::getRank));
        }
    }

    public List<Criteria> getCriteria() {
        return this.criteria;
    }

    public List<Alternative> getAlternatives() {
        return this.alternatives;
    }

    public boolean isDataNotOk() {
        return !this.isDataOk;
    }
}
