package validator;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class ValidationEngine {

    //Rule model
    static class Rule {
        boolean required;
        Pattern regex;
        String type;
    }

    public static void main(String[] args) {

        //Create output directory if not exists
        new File("output").mkdirs();

        // File paths
        String inputCsv  = "resources/raw.csv";
        String rulesCsv  = "resources/rules.csv";

        String cleanCsv  = "output/clean.csv";
        String rejectCsv = "output/rejects.csv";
        String auditTxt  = "output/audit.txt";
        String statsCsv  = "output/stats.csv";

        // Rule storage
        Map<Integer, Rule> rulesMap = new HashMap<>();

        int totalRows = 0;
        int validRows = 0;
        int invalidRows = 0;
        int rowNumber = 0;


        // 1️⃣ LOAD RULES FROM rules.csv
        try (BufferedReader br = new BufferedReader(new FileReader(rulesCsv))) {

            br.readLine(); // skip header
            String line;
            int colIndex = 0;

            while ((line = br.readLine()) != null) {

                String[] r = line.split(",", -1);

                Rule rule = new Rule();
                rule.required = r[1].equalsIgnoreCase("true");

                if (!r[2].isEmpty())
                    rule.regex = Pattern.compile(r[2]);

                if (!r[3].isEmpty())
                    rule.type = r[3];

                rulesMap.put(colIndex++, rule);
            }

        } catch (IOException e) {
            System.out.println("❌ Error reading rules.csv");
            e.printStackTrace();
            return;
        }

       
        // 2️⃣ PROCESS INPUT CSV
        try (
            BufferedReader br = new BufferedReader(new FileReader(inputCsv));
            BufferedWriter cleanWriter  = new BufferedWriter(new FileWriter(cleanCsv));
            BufferedWriter rejectWriter = new BufferedWriter(new FileWriter(rejectCsv));
            BufferedWriter auditWriter  = new BufferedWriter(new FileWriter(auditTxt))
        ) {

            String header = br.readLine();

            cleanWriter.write(header);
            cleanWriter.newLine();

            rejectWriter.write(header);
            rejectWriter.newLine();

            String line;

            while ((line = br.readLine()) != null) {

                rowNumber++;
                totalRows++;

                String[] values = line.split(",", -1);
                boolean valid = true;
                StringBuilder error = new StringBuilder();

                for (int i = 0; i < rulesMap.size(); i++) {

                    Rule rule = rulesMap.get(i);
                    String value = (i < values.length)
                            ? values[i].replace("\"", "").trim()
                            : "";

                    //Required check
                    if (rule.required && value.isEmpty()) {
                        valid = false;
                        error.append("Column ").append(i + 1).append(" required. ");
                    }

                    //Regex check
                    if (!value.isEmpty() && rule.regex != null &&
                            !rule.regex.matcher(value).matches()) {
                        valid = false;
                        error.append("Column ").append(i + 1).append(" invalid format. ");
                    }

                    //Type check
                    if (!value.isEmpty() && "int".equalsIgnoreCase(rule.type)) {
                        try {
                            Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            valid = false;
                            error.append("Column ").append(i + 1).append(" not integer. ");
                        }
                    }
                }

                //Write output
                if (valid) {
                    validRows++;
                    cleanWriter.write(line);
                    cleanWriter.newLine();
                } else {
                    invalidRows++;
                    rejectWriter.write(line);
                    rejectWriter.newLine();
                    auditWriter.write("Row " + rowNumber + ": " + error);
                    auditWriter.newLine();
                }
            }

        } catch (IOException e) {
            System.out.println("❌ Error processing input CSV");
            e.printStackTrace();
        }

     
        // 3️⃣ WRITE STATS
 
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(statsCsv))) {

            bw.write("Total Rows,Valid Rows,Invalid Rows,Rejection Rate (%)");
            bw.newLine();

            double rate = totalRows == 0 ? 0 : (invalidRows * 100.0) / totalRows;
            bw.write(totalRows + "," + validRows + "," + invalidRows + "," + rate);

        } catch (IOException e) {
            System.out.println("❌ Error writing stats.csv");
            e.printStackTrace();
        }


        // ✅ SUCCESS MESSAGE
        System.out.println("==============================================");
        System.out.println("✅ CSV Validation Successfully Completed");
        System.out.println("Total Rows   : " + totalRows);
        System.out.println("Valid Rows   : " + validRows);
        System.out.println("Invalid Rows : " + invalidRows);
        System.out.println("Output files generated in /output folder");
        System.out.println("==============================================");
    }
}
