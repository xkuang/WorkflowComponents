package edu.cmu.pslc.learnsphere.analysis.lsa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.learnsphere.analysis.lsa.LSAUtil;

public class LsaMain extends AbstractComponent {

    public static LSAUtil lsa;

    public static void main(String[] args) {

        LsaMain tool = new LsaMain();
        tool.startComponent(args);
    }

    public LsaMain() {
        super();
    }

    @Override
    protected void runComponent() {
        // Parse arguments
        File inputFile = null;
        String col1 = null;
        String col2 = null;
        String corpus = null;
        int lag = 0;
        String returnvals = null;
        String simfunc = null;

        col1 = this.getOptionAsString("header1");
        col2 = this.getOptionAsString("header2");
        returnvals = this.getOptionAsString("returnvals");
        lag = this.getOptionAsInteger("lag");
        returnvals = this.getOptionAsString("returnvals");
        corpus = this.getOptionAsString("corpus");
        simfunc = this.getOptionAsString("simfunc");

        inputFile = this.getAttachment(0,  0);

        try {
            lsa = new LSAUtil();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //loding semantic space to the program from given directory
        try {
            //lsa.loadSpace(this.getToolDir() + "/program");
        	lsa.loadSpace("/datashop/SemanticSpace/"+corpus);  
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //Generating required out
        try {
            lSAsimilarityCalc(inputFile, col1, col2, lag, returnvals, simfunc);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        System.out.println(this.getOutput());

        for (String err : this.errorMessages) {
            // These will also be picked up by the workflows platform and relayed to the user.
            System.err.println(err);
        }
    }


    public void lSAsimilarityCalc(File inputFile, String col1,
            String col2, int lac, String returnvals, String simfunc)
            throws Exception {
        if (inputFile == null) {
            this.addErrorMessage("Invalid Input File.");
            return;
        }

        if (!inputFile.exists() || !inputFile.canRead()) {
            this.addErrorMessage("Input File not found or could not be read.");
            return;
        }

        File generatedFile = this.createFile(
                "Step-values-with-predictions", ".txt");

        FileWriter oStream = new FileWriter(generatedFile);
        BufferedWriter sw = new BufferedWriter(oStream);
        BufferedReader sr = new BufferedReader(new FileReader(inputFile));

        String line1 = sr.readLine().trim();
        String[] st1 = line1.split("\t");

        int indexCol1;
        int indexCol2;
        int indexCol3;
        try {
            // for Anon Student Id
            indexCol1 = Arrays.asList(st1).indexOf(col1);
            indexCol2 = Arrays.asList(st1).indexOf(col2);
            indexCol3 = Arrays.asList(st1).indexOf(
                    "Anon Student Id");


        } catch (Exception e) {
            if (col1 != null)
                indexCol1 = Arrays.asList(st1).indexOf(col1);
            else
                indexCol1 = -1;
            if (col2 != null)
                indexCol2 = Arrays.asList(st1).indexOf(col2);
            else
                indexCol2 = -1;
            indexCol3 = Arrays.asList(st1).indexOf(
                    "Anon Student Id");
        }

        // Case handling based on requested output format
        switch (returnvals) {
        case "col":
            if (indexCol1 == -1) {
                indexCol1 = indexCol2;
            }

            if (indexCol2 == -1) {
                indexCol2 = indexCol1;
            }

            sw.write(line1+"\tCF(Similarity value of "+col1+" and "+col2+")");
            sw.newLine();
            // Console.WriteLine(col1 + "\t" + col2 + "\t" +
            // "Similarity value");

            if (lac == 0) {
                String line = sr.readLine();

                int l = 1;
                while (line != null) {
                    // int p = 0;
                    line = line.trim();

                    if (line.equals(""))
                        continue;

                    String[] st = line.split("\t");

                    String term1 = st[indexCol1].trim();
                    String term2 = st[indexCol2].trim();
                    if (term1.equals("") || term2.equals("")) {
                        sw.write(line + "\t" + "NA");
                        sw.newLine();
                    } else {
                        float x;
                        if (simfunc.equals("cosine")) {
                            x = lsa.getCosine(term1, term2);
                        } else if(simfunc.equals("euclidean")){
                            x = lsa.getEuclidean(term1, term2);
                        }else {
                            this.addErrorMessage("Requested Similarity function not available");  
                            return;
                        }

                        sw.write(line+ "\t"+ String.valueOf(x));
                        sw.newLine();
                    }
                    line = sr.readLine();
                    l++;
                    //System.out.println(l);
                }
                break;
            } else {
                // if (p < 10) Console.WriteLine(term1 + "\t" + term2 + "\t" +
                // x.ToString());
                // p++;
                // string[] s = { term2, x.ToString() };
                // stringsToComp.Add(term1,s);
                // Console.WriteLine("10 samples printed out of " + p +
                // ". Please check output file for all output. \n\nLocation: \n"
                // + outputFile);
                if (indexCol3 == -1) {
                    this.addErrorMessage("Anon Student Id required for this operation");
                    return;
                }
                
                List<String> allLines = new ArrayList<String>();
                List<String> lines1 = new ArrayList<String>();
                List<String> lines2 = new ArrayList<String>();
                List<String> lines3 = new ArrayList<String>();
                HashMap<String, List<String>> dataMatrix = new HashMap<String, List<String>>();

                String line = sr.readLine();
                while (line != null) {
                    line = line.trim();
                    if (line.equals(""))
                        continue;
                    allLines.add(line);

                    String[] st = line.split("\t");
                    String term1;
                    String term2;
                    term1 = st[indexCol1].trim();
                    term2 = st[indexCol2].trim();
                    String term3 = st[indexCol3].trim();
                    lines1.add(term1.trim());
                    lines2.add(term2.trim());
                    lines3.add(term3.trim());
                    line = sr.readLine();
                }
                if (lines1.size() < 1) {
                    this.addErrorMessage("No entry in given column1");
                    return;
                }
                if (lines2.size() < 1) {
                    this.addErrorMessage("No entry in given column2");
                    return;
                }

                // int k = 0;
                String student = " ";
                int l = 0;
                for (int i = 0; i < lines2.size(); i++) {
                    if (!student.equals(lines3.get(i))) {
                        student = lines3.get(i);
                        l = 1;
                        sw.write(allLines.get(i)+"\t" + "NA");
                        sw.newLine();
                        continue;
                    }

                    if (l < lac) {
                        l++;
                        sw.write(allLines.get(i)+"\t" + "NA");
                        sw.newLine();
                        continue;
                    }

                    String v = lines2.get(i).trim();
                    String u = lines1.get(i - lac).trim();
                    if (i < lac || u.equals("") || v.equals("")) {
                        sw.write(allLines.get(i) + "\t" + "NA");
                        sw.newLine();
                    } else {
                        float x = 0;
                        if (simfunc.equals("cosine")) {
                            x = lsa.getCosine(u, v);
                        } else if(simfunc.equals("euclidean")){
                            x = lsa.getEuclidean(u, v);
                        } else {
                            this.addErrorMessage("Requested Similarity function not available");
                            return;
                        }
                        sw.write(allLines.get(i) + "\t" + String.valueOf(x));
                        sw.newLine();
                    }
                }
                // if (k < 10) Console.WriteLine(u + "\t" + v + "\t" +
                // x.ToString());
                // k++;
                //System.out.println();
                break;
            }
            // Console.WriteLine("10 samples printed out of. Please check output file for all output. \n\nLocation: \n"
            // + outputFile);
        case "uniqmat":
            if (indexCol1 == -1) {
                indexCol1 = indexCol2;
            }

            if (indexCol2 == -1) {
                indexCol2 = indexCol1;
            }
            
            //Use HashSet for unique value

            //HashSet<String> lin1 = new HashSet<String>();
            //HashSet<String> lin2 = new HashSet<String>();
            
            List<String> lin1 = new ArrayList<String>();
            List<String> lin2 = new ArrayList<String>();

            
            String line = sr.readLine();
            while (line != null) {
                line = line.trim();
                if (line.equals(""))
                    continue;

                String[] st = line.split("\t");
                String term1;
                String term2;
                try {
                    term1 = st[indexCol1].trim();
                } catch (Exception e) {
                    term1 = "";
                }

                try {
                    term2 = st[indexCol2].trim();
                } catch (Exception e) {
                    term2 = "";
                }
                
                if (!term1.equals("") && !lin1.contains(term1))
                    lin1.add(term1);
                
                if (!term2.equals("") && !lin2.contains(term2))
                    lin2.add(term2);

                line = sr.readLine();
            }
            if (lin1.size() < 1) {
                this.addErrorMessage("No entry in given column1");
                return;
            }

            if (lin2.size() < 1) {
                this.addErrorMessage("No entry in given column2");
                return;
            }
            
            sw.write("\t");
            for (String v : lin2) {
                sw.write(v +"\t");
            }
            sw.newLine();

            HashMap<Integer, List<Float>> matrix = new HashMap<Integer, List<Float>>();
            int h = 0;
            for (String u : lin1) {
                List<Float> x = new ArrayList<Float>();
                // x.Add(1F);
                u.trim();
                for (String v : lin2) {
                    v.trim();
                    if (simfunc.equals("cosine")) {
                        x.add(lsa.getCosine(u, v));
                    } else if(simfunc.equals("euclidean")){
                        x.add(lsa.getEuclidean(u, v));
                    } else {
                        this.addErrorMessage("Requested Similarity function not available");
                        return;
                    }
                }
                matrix.put(h, x);
                h++;
            }
            for (int i = 0; i < lin1.size(); i++) {
                sw.write(lin1.get(i)+"\t");
                for (int j = 0; j < lin2.size(); j++) {
                    sw.write( matrix.get(i).get(j)+"\t");
                }
                if (i < lin1.size()){sw.newLine();}
            }
            break;
        default:
            this.addErrorMessage("Invalid returnvals type");

        }
        sr.close();
        sw.close();
        oStream.close();

        if (this.isCancelled()) {
            this.addErrorMessage("Cancelled workflow during component execution.");
        } else{
        	if (returnvals.equals("col")){
            Integer nodeIndex = 0;
            Integer fileIndex = 0;
            String fileType = "student-step";
            this.addOutputFile(generatedFile, nodeIndex, fileIndex, fileType);
        	}
            else if (returnvals.equals("uniqmat"))  {
	            Integer nodeIndex = 0;
	            Integer fileIndex = 0;
	            String fileType = "text";
	            this.addOutputFile(generatedFile, nodeIndex, fileIndex, fileType);
            }
            else{this.addErrorMessage("No output Generated for return type: "+returnvals);}
        }

}
