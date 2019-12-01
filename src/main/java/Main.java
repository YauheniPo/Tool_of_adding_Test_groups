import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    private static HashMap<String, List<String>> mappingComponent = new HashMap<>();
    private static String projectPath = "D:\\g2-e2e-test";

    public static void main(String[] args) throws IOException {
        String testsPath = projectPath + "\\src\\main\\java\\com\\mscs\\emr\\test\\functional\\g2\\test\\";
        String mappingExcelFilePath = "C:\\Users\\Yauheni_Papovich1\\Documents\\G2_TF_vs_Automated_TCs - Copy.xlsx";

        List<String> codeFilePaths = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(Paths.get(testsPath))) {
            codeFilePaths.addAll(walk.map(Path::toString).filter(f -> f.endsWith(".java")).collect(Collectors.toList()));

        } catch (IOException e) {
            e.printStackTrace();
        }

        readMappingExcelFile(mappingExcelFilePath);
        Set<String> components = mappingComponent.keySet();
        for (String codeFilePath : codeFilePaths) {
            for (String component : components) {
                for (String classPath : mappingComponent.get(component)) {
                    if (codeFilePath.equals(testsPath + classPath)) {
                        replaceText(codeFilePath, component);
                    }
                }
            }
        }
        updateTestGroupsClass(components);
    }


    private static void readMappingExcelFile(String mappingExcelFilePath) throws IOException {
        Workbook workbook = WorkbookFactory.create(new File(mappingExcelFilePath));

        Sheet sheet = workbook.getSheet("Sheet2");

        for (Row row : sheet) {
            String componentName = row.getCell(0).getStringCellValue();
            String testClass = row.getCell(1).getStringCellValue();

            List<String> testFailedBySpecifiedMethod = mappingComponent.containsKey(componentName) ? mappingComponent.get(componentName) : new ArrayList<>();
            testFailedBySpecifiedMethod.add(testClass);
            mappingComponent.put(componentName, testFailedBySpecifiedMethod);
        }
    }

    private static String getFileContent(String filepath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(filepath)));

        String line;
        StringBuilder stringBuilderClassContent = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            stringBuilderClassContent.append(line).append("\r\n");
        }
        reader.close();
        return stringBuilderClassContent.toString();
    }

    private static void replaceText(String file, String componentGroup) {
        try {
            String classContent = getFileContent(file);
            String regExp = "(groups.=.\\{)(.*?)}";
            String groups = getMatcherGroupContent(regExp, classContent, 1);
            String groupsWithComponent = String.format("TestGroup.%s", componentGroup);
            String newClassContent = classContent.replace(groups, Matcher.quoteReplacement(groups + groupsWithComponent + ", "));

            Pattern pattern = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(newClassContent);
            while (matcher.find()) {
                String group = matcher.group(2);
                newClassContent = newClassContent.replaceAll(group, group.replaceAll("  ", " "));
            }

            writeFileContent(file, newClassContent);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static void updateTestGroupsClass(Set<String> components) {
        try {
            String testGroupsClassPath = projectPath + "\\src\\main\\java\\com\\mscs\\emr\\test\\utils\\TestGroup.java";
            String classContent = getFileContent(testGroupsClassPath);
            String regExp = "TestGroup.\\{(.*?)}";
            String groupsVariables = getMatcherGroupContent(regExp, classContent, 1);
            String groupsVariablesWithComponent = groupsVariables + components.stream()
                    .map(component -> String.format("\tpublic static final String %1$s = \"%s\";", component))
                    .collect(Collectors.joining("\r\n")) + "\r\n";
            classContent = classContent.replace(groupsVariables, Matcher.quoteReplacement(groupsVariablesWithComponent));
            writeFileContent(testGroupsClassPath, classContent);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static String getMatcherGroupContent(String regExp, String text, int index) {
        Pattern pattern = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        matcher.find();
        return matcher.group(index);
    }

    private static void writeFileContent(String file, String content) throws IOException {
        FileWriter writer = new FileWriter(file);
        writer.write(content);
        writer.close();
    }
}


