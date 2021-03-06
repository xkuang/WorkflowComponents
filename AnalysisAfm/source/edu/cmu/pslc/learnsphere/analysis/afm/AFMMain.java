package edu.cmu.pslc.learnsphere.analysis.afm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import edu.cmu.pslc.afm.dataObject.AFMDataObject;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.dataStructure.TrainingResult;

// The PenalizedAFMTransferModel applies the AFM to the student-step data.
import edu.cmu.pslc.afm.transferModel.AFMTransferModel;
import edu.cmu.pslc.afm.transferModel.PenalizedAFMTransferModel;
// The AbstractComponent class is required by each component.
import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: Additive Factor Model (AFM) main class.
 */
public class AFMMain extends AbstractComponent {

    /** The string split parameter, used to split AFM results;
        see http://docs.oracle.com/javase/7/docs/api/java/lang/String.html. */
    private static final Integer PATTERN_NO_LIMIT = -1;

    /** Decimal format used for predicted error rates. */
    private DecimalFormat decimalFormat;

    /** Decimal format used for KC Model values. */
    private DecimalFormat kcmFormat;

    /** Decimal format used for integer values. */
    private DecimalFormat integerFormat;

    /** Component option (model). */
    String modelName = null;

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        AFMMain tool = new AFMMain();
        tool.startComponent(args);

    }

    /**
     * This class runs AFM one or more times depending on the number of input elements.
     */
    public AFMMain() {
        super();
    }


    /**
     * The test() method is used to test the known inputs prior to running.
     * @return true if passing, false otherwise
     */
    @Override
    protected Boolean test() {
        Boolean passing = true;

        return passing;
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");
        // Add the column headers from our input file to this component's output metadata,
        // plus one extra column for Predicted Error Rate, if it doesn't already exist.

        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
        this.addMetaDataFromInput("student-step", 0, 0, ".*");
        this.addMetaData("student-step", 0, META_DATA_LABEL, "label0", 0, "Predicted Error Rate (" + modelName + ")");
    }

    @Override
    protected void parseOptions() {

        if (this.getOptionAsString("model") != null) {
            modelName = this.getOptionAsString("model").replaceAll("(?i)\\s*KC\\s*\\((.*)\\)\\s*", "$1");
        }
    }

    /**
     * Processes the student-step file and associated model name to generate
     * the inputs to the next component.
     */
    @Override
    protected void runComponent() {

        File predictedErrorRateFile = this.createFile("Step-values-with-predictions", ".txt");
        File modelValuesFile = this.createFile("KC-model-values", ".txt");
        File parametersFile = this.createFile("Parameter-estimate-values", ".txt");

        // The decimal format for predicted error rates.
        decimalFormat = new DecimalFormat("0.000#");
        kcmFormat = new DecimalFormat("#,###.##");
        integerFormat = new DecimalFormat("#,###");

        AFMTransferModel theModel = null;
        List<Long> invalidLines = new ArrayList<Long>();

        try {
            // Instantiate the AFM class
            PenalizedAFMTransferModel penalizedAFMTransferModel = new PenalizedAFMTransferModel();

            // Run the Penalized AFM Transfer Model
            File steprollup =this.getAttachment(0, 0);

            theModel = penalizedAFMTransferModel.runAFM(
                steprollup, modelName, Collections.synchronizedList(invalidLines));

        } catch (Exception e) {
            logger.error("Exception caused by AFM." + e);
            e.printStackTrace();
        }

        TrainingResult trainingResult = theModel.getTrainingResult();
        ;

        if ((theModel != null) && (trainingResult != null)) {

            // Write the predicted error rates to the first output file.
            predictedErrorRateFile = populatePredictedErrorRateFile(predictedErrorRateFile,
                trainingResult, invalidLines);

            // Now, write the model values to the second output file.
            modelValuesFile = populateModelValuesFile(modelValuesFile, theModel);

            // Finally, write the parameter estimate values to the third output file.
            parametersFile = populateParametersFile(parametersFile, theModel);

        } else {
            this.addErrorMessage("The results from AFM were empty.");
        }

        if (this.isCancelled()) {
            this.addErrorMessage("Cancelled workflow during component execution.");
        } else {
            Integer nodeIndex = 0;
            Integer fileIndex = 0;
            String fileType = "student-step";
            this.addOutputFile(predictedErrorRateFile, nodeIndex, fileIndex, fileType);
            nodeIndex = 1;
            fileType = "model-values";
            this.addOutputFile(modelValuesFile, nodeIndex, fileIndex, fileType);
            nodeIndex = 2;
            fileType = "parameters";
            this.addOutputFile(parametersFile, nodeIndex, fileIndex, fileType);
        }

        System.out.println(this.getOutput());


        for (String err : this.errorMessages) {
            // These will also be picked up by the workflows platform and relayed to the user.
            System.err.println(err);
        }

    }

    // Constant
    private static final String NEW_LINE_CHAR = "\n";

    // Constant
    private static final String TAB_CHAR = "\t";

    /**
     * Write the Predicted Error Rate values to a file.
     * @param theFile the File to write to
     * @param tr the AFMTransferModel Training Result
     * @return the populated file
     */
    private File populatePredictedErrorRateFile(File theFile, TrainingResult tr,
            List<Long> invalidLines) {

        double[] pe = tr.getPredictedError();

        if (pe == null) {
            this.addErrorMessage("Predicted error rate results from AFM were empty.");
            return theFile;
        }

        // Comma-delimited list of predicted error rates
        String predictedErrorRates = Arrays.toString(pe);

        // Convert the values to a list of doubles for formatting
        List<Double> doubleValues = new ArrayList<Double>();
        String[] stringValues = predictedErrorRates.replaceAll("[\\[\\]]+", "")
                    .split("[\\s,]+", PATTERN_NO_LIMIT); // split with empty
                                                         // strings allowed,
                                                         // e.g. 3, 4, , 5 is interpreted as
                                                         // "3","4","","5"

        for (String stringValue : stringValues) {
            doubleValues.add(Double.parseDouble(stringValue));
        }

        // Java try-with-resources
        try (OutputStream outputStream = new FileOutputStream(theFile);
             FileInputStream inStream = new FileInputStream(this.getAttachment(0, 0));
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inStream))) {

                // Write header to export
                String headerLine = bufferedReader.readLine();
                String[] headerArray = headerLine.split(TAB_CHAR);
                Integer headerCount = 0;
                Integer replaceHeaderIndex = headerArray.length;
                for (String header : headerArray) {
                    if (header.equalsIgnoreCase("Predicted Error Rate ("
                            + modelName + ")")) {
                        replaceHeaderIndex = headerCount;
                    }
                    headerCount++;
                }

                if (replaceHeaderIndex == headerArray.length) {
                    String newHeader = headerLine + TAB_CHAR
                            + "Predicted Error Rate (" + modelName
                            + ")\n";
                    outputStream.write(newHeader.getBytes("UTF-8"));
                } else {
                    outputStream.write(headerLine.getBytes("UTF-8"));
                }

                outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));

                // Write values to export
                String line = bufferedReader.readLine();
                Long lineCount = 0L;
                Long validLineCount = -1L;

                while (line != null) {
                    Boolean isValidLine = true;
                    lineCount++; // because we already read the header line

                    if (invalidLines.contains(lineCount)) {
                        isValidLine = false;
                    } else {
                        validLineCount++;
                    }

                    String predictedValueString = null;

                    if (isValidLine) {
                        Integer validLineIndex = validLineCount.intValue();
                        predictedValueString = decimalFormat
                            .format(doubleValues.get(validLineIndex));
                    } else {
                        predictedValueString = "";
                    }
                    String[] valueArray = line.split(TAB_CHAR);


                    Integer colIndex = 0;
                    for (String value : valueArray) {
                        byte[] bytes = null;

                        if (replaceHeaderIndex < valueArray.length
                                && replaceHeaderIndex == colIndex) {
                            bytes = (predictedValueString + TAB_CHAR)
                                    .getBytes("UTF-8");
                        } else {
                            bytes = (value + TAB_CHAR).getBytes("UTF-8");
                        }

                        outputStream.write(bytes);
                        colIndex++;
                    }
                    if (replaceHeaderIndex == headerArray.length) {
                        outputStream.write(predictedValueString
                                .getBytes("UTF-8"));
                    }

                    outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));
                    line = bufferedReader.readLine();
                }
            } catch (Exception e) {
                // This will be picked up by the workflows platform and relayed to the user.
                e.printStackTrace();
            }

        return theFile;
    }

    /**
     * Write the KC Model values to a file.
     * @param theFile the File to write to
     * @param model the PenalizedAFMTransferModel
     * @return the populated file
     */
    private File populateModelValuesFile(File theFile, AFMTransferModel model) {

        TrainingResult tr = model.getTrainingResult();

        Double logLikelihood = tr.getLogLikelihood();
        Double aic = tr.getAIC();
        Double bic = tr.getBIC();
        Integer numObs = model.getNumberOfObservationWithKC();

        AFMDataObject ado = model.getAFMDataObject();

        // Are these all or nothing?
        if (ado == null || logLikelihood == null || aic == null || bic == null) {
            this.addErrorMessage("Model value results from AFM were empty.");
            return theFile;
        }

        Integer numSkills = ado.getSkills().size();
        Integer numStudents = ado.getStudents().size();

        String kcPrefix = "KC Model Values for " + modelName + " model: ";

        // Java try-with-resources
        try (OutputStream outputStream = new FileOutputStream(theFile)) {

                // Write values to export
                byte[] label = null;
                byte[] value = null;

                // AIC
                label = (kcPrefix + "AIC" + TAB_CHAR).getBytes("UTF-8");
                outputStream.write(label);
                value = (kcmFormat.format(aic)).getBytes("UTF-8");
                outputStream.write(value);
                outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));

                // BIC
                label = (kcPrefix + "BIC" + TAB_CHAR).getBytes("UTF-8");
                outputStream.write(label);
                value = (kcmFormat.format(bic)).getBytes("UTF-8");
                outputStream.write(value);
                outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));

                // Log Likelihood
                label = (kcPrefix + "Log Likelihood" + TAB_CHAR).getBytes("UTF-8");
                outputStream.write(label);
                value = (kcmFormat.format(logLikelihood)).getBytes("UTF-8");
                outputStream.write(value);
                outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));

                // Number of parameters
                Integer numParameters = 2*numSkills + numStudents;
                label = (kcPrefix + "Number of Parameters" + TAB_CHAR).getBytes("UTF-8");
                outputStream.write(label);
                value = (integerFormat.format(numParameters)).getBytes("UTF-8");
                outputStream.write(value);
                outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));

                // Number of Observations
                label = (kcPrefix + "Number of Observations" + TAB_CHAR).getBytes("UTF-8");
                outputStream.write(label);
                value = (integerFormat.format(numObs)).getBytes("UTF-8");
                outputStream.write(value);
                outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));

            } catch (Exception e) {
                // This will be picked up by the workflows platform and relayed to the user.
                e.printStackTrace();
            }

        return theFile;
    }

    /**
     * Write the Intercept and Slope values to a file.
     * @param theFile the File to write to
     * @param model the PenalizedAFMTransferModel
     * @return the populated file
     */
    private File populateParametersFile(File theFile, AFMTransferModel model) {

        AFMDataObject ado = model.getAFMDataObject();

        if (ado == null) {
            this.addErrorMessage("Parameter estimate results from AFM were empty.");
            return theFile;
        }

        List<String> skillNames = ado.getSkills();
        double[] skillParams = model.getSkillParameters();
        List<String> studentNames = ado.getStudents();
        double[] stuParams = model.getStudentParameters();

        // Java try-with-resources
        try (OutputStream outputStream = new FileOutputStream(theFile)) {

                // Header: Type, Name, Intercept, Slope
                byte[] header = ("Type" + TAB_CHAR + "Name" + TAB_CHAR
                                 + "Intercept" + TAB_CHAR + "Slope")
                    .getBytes("UTF-8");
                outputStream.write(header);
                outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));

                // Write values to export
                byte[] label = null;
                byte[] value = null;

                int count = 0;
                for (String s : skillNames) {
                    label = ("Skill" + TAB_CHAR + s + TAB_CHAR).getBytes("UTF-8");
                    outputStream.write(label);
                    double intercept = getIntercept(count, skillParams);
                    double slope = getSlope(count, skillParams);
                    value = (kcmFormat.format(intercept) + TAB_CHAR
                             + decimalFormat.format(slope)).getBytes("UTF-8");
                    outputStream.write(value);
                    outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));

                    count++;
                }

                count = 0;
                // Null slope for students
                for (String s : studentNames) {
                    label = ("Student" + TAB_CHAR + s + TAB_CHAR).getBytes("UTF-8");
                    outputStream.write(label);
                    value = (kcmFormat.format(stuParams[count]) + TAB_CHAR).getBytes("UTF-8");
                    outputStream.write(value);
                    outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));

                    count++;
                }

            } catch (Exception e) {
                // This will be picked up by the workflows platform and relayed to the user.
                e.printStackTrace();
            }

        return theFile;
    }

    /**
     * Get the intercept (logit) value, aka 'Beta'.
     * @param index the index into the skillParams array
     * @param skillParams the array of values, twice the number of skills
     * @param the intercept
     */
    private double getIntercept(int index, double[] skillParams) {
        return skillParams[index * 2];
    }

    /**
     * Get the slope value, aka 'Gamma'.
     * @param index the index into the skillParams array
     * @param skillParams the array of values, twice the number of skills
     * @param the slope
     */
    private double getSlope(int index, double[] skillParams) {
        return skillParams[index * 2 + 1];
    }
}
