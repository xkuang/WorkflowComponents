package edu.cmu.pslc.learnsphere.analysis.datalab;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

import edu.cmu.datalab.util.Gradebook;
import edu.cmu.datalab.util.GradebookUtils;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Workflow component: Datalab Cronbach's Alpha tool.
 */
public class CronbachsAlphaMain extends AbstractComponent {

    private String[] headers;
    private String[] students;
    private int numItems = 0;
    private int numStudents = 0;

    public static void main(String[] args) {

        CronbachsAlphaMain tool = new CronbachsAlphaMain();
        tool.startComponent(args);
    }

    public CronbachsAlphaMain() {
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

    /**
     * Processes the student-step file and associated model name to generate
     * the inputs to the next component.
     */
    @Override
    protected void runComponent() {

        File outputFile = this.createFile("CronbachsAlpha", ".txt");

        Gradebook gradebook = GradebookUtils.readFile(this.getAttachment(0, 0));

        Array2DRowRealMatrix data = gradebook.getData();
        headers = gradebook.getHeaders();
        students = gradebook.getStudents();
        numItems = headers.length - 1;  // don't include student column
        numStudents = students.length;

        Boolean summaryColPresent =
            this.getOptionAsString("summary_column_present").equalsIgnoreCase("true");
        logger.info("*** summaryColPresent = " + summaryColPresent);

        // Write the output file.
        outputFile = populateCronbachsAlphaFile(outputFile, data);

        if (this.isCancelled()) {
            this.addErrorMessage("Cancelled workflow during component execution.");
        } else {
            Integer nodeIndex = 0;
            Integer fileIndex = 0;
            String fileType = "cronbachs-alpha";
            this.addOutputFile(outputFile, nodeIndex, fileIndex, fileType);
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
     * Write the CronbachsAlpha values to a file.
     * @param theFile the File to write to
     * @param data the matrix of data
     * @return the populated file
     */
    private File populateCronbachsAlphaFile(File theFile, Array2DRowRealMatrix data) {

        // Java try-with-resources
        try (OutputStream outputStream = new FileOutputStream(theFile)) {
                
                // Write values to export
                Double[] cronbachValues = getCronbachValues(data);
                for (int i = 0; i < numItems; i++) {
                    String label = "All items";
                    if (i > 0) {
                        label = headers[i].trim() + " excluded";
                    }
                    outputStream.write(label.getBytes("UTF-8"));

                    outputStream.write(TAB_CHAR.getBytes("UTF-8"));
                    outputStream.write(String.valueOf(cronbachValues[i]).getBytes("UTF-8"));
                    outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));
                }

            } catch (Exception e) {
                // This will be picked up by the workflows platform and relayed to the user.
                e.printStackTrace();
            }

        return theFile;
    }

    /**
     * Compute Cronbach's alpha for whole set and for eliminating each item
     * @param data Array2DRowRealMatrix
     * @return double[] with n+1 values where n is the number of columns of data.
     *  The first value is for the whole set, the rest is for eliminating each column
     * 
     */
    private Double[] getCronbachValues (Array2DRowRealMatrix data) {
        int allRowCnt = data.getRowDimension();
        int rowCnt = allRowCnt;
        int allColumnCnt = data.getColumnDimension();
        int columnCnt = allColumnCnt;
        //keep count of rows for each column
        double[] rowCounts = new double[allColumnCnt];
        //column variance among rows
        double[] varOfColumn = new double[allColumnCnt];
        //keep count of columns for each row
        double[] columnCounts = new double[allRowCnt];
        //total and average for each rows
        double[] rowTotal = new double[allRowCnt];
        double[] rowAverage = new double[allRowCnt];
        double[] rowAvgMultiByColumnCount = new double[allRowCnt];
        //set column count, total, avg*columnCount for each row
        //for row with only one column, set all measures to NaN
        for (int i = 0; i < allRowCnt; i++) {
            double[] thisRow = data.getRow(i);
            ArrayList<Double> thisRowAL = new ArrayList<Double>();
            for (int j = 0; j < thisRow.length; j++) {
                if (!Double.isNaN(thisRow[j])) {
                    thisRowAL.add(thisRow[j]);
                }
            }
            double[] rowValues =  ArrayUtils.toPrimitive(thisRowAL.toArray(new Double[0]));
            if (rowValues == null) {
                columnCounts[i] = Double.NaN;
            } else {
                columnCounts[i] = rowValues.length;
            }
            if (rowValues == null || rowValues.length < 2) {
                rowCnt--;
                rowTotal[i] = Double.NaN;
                rowAverage[i] = Double.NaN;
                rowAvgMultiByColumnCount[i] = Double.NaN;
            } else {
                rowTotal[i] = StatUtils.sum(rowValues);
                rowAverage[i] = StatUtils.mean(rowValues);
            }
        }

        //set row count and variance for each column
        for (int i = 0; i < allColumnCnt; i++) {
            double[] thisCol = data.getColumn(i);
            ArrayList<Double> thisColAL = new ArrayList<Double>();
            for (int j = 0; j < thisCol.length; j++) {
                if (!Double.isNaN(thisCol[j]))
                    thisColAL.add(thisCol[j]);
            }
            double[] colValues = ArrayUtils.toPrimitive(thisColAL.toArray(new Double[0]));
            if (colValues == null) {
                rowCounts[i] = Double.NaN;
            } else {
                rowCounts[i] = colValues.length;
            }
            if (colValues == null || colValues.length < 2) {
                columnCnt--;
                varOfColumn[i] = Double.NaN;
            } else {
                varOfColumn[i] = StatUtils.variance(colValues);
            }
        }

        //rowAvgMultiByColumnCount is (row's mean)*columnCnt
        for (int i = 0; i < allRowCnt; i++) {
            if (!Double.isNaN(rowAverage[i])) {
                rowAvgMultiByColumnCount[i] = rowAverage[i]*(double)columnCnt;
            } else {
                rowAvgMultiByColumnCount[i] = Double.NaN;
            }
        }
        double[] returnValues = new double[1 + allColumnCnt];
        double sumOfColumnVariance = 0.0;
        //compute alpha for whole set
        if (rowCnt < 2 || columnCnt < 2) {
            returnValues[0] = Double.NaN;
            sumOfColumnVariance = Double.NaN;
        } else {
            //compute variance with rowAvgMultiByColumnCount
            //exclude NaN
            ArrayList<Double> rowTotalAL = new ArrayList<Double>();
            for (int i = 0; i < rowAvgMultiByColumnCount.length; i++) {
                if (!Double.isNaN(rowAvgMultiByColumnCount[i]))
                    rowTotalAL.add(rowAvgMultiByColumnCount[i]);
            }
            if (rowTotalAL.size() < 2) {
                returnValues[0] = Double.NaN;
            } else {
                double[] rowTotalValues = ArrayUtils.toPrimitive(rowTotalAL.toArray(new Double[0]));
                double varianceOfTotal = StatUtils.variance(rowTotalValues);
                //sum of column variance
                //exclude NaN
                ArrayList<Double> varOfColumnAL = new ArrayList<Double>();
                for (int i = 0; i < varOfColumn.length; i++) {
                    if (!Double.isNaN(varOfColumn[i])) {
                        varOfColumnAL.add(varOfColumn[i]);
                    }
                }
                if (varOfColumnAL.size() < 2) {
                    returnValues[0] = Double.NaN;
                } else {
                    double[] varOfColumnValues =
                        ArrayUtils.toPrimitive(varOfColumnAL.toArray(new Double[0]));
                    sumOfColumnVariance = StatUtils.sum(varOfColumnValues);
                    double value = computeAlphaValue(columnCnt,
                                                     sumOfColumnVariance, varianceOfTotal);
                    if (Double.isInfinite(value) || Double.isNaN(value)) {
                        returnValues[0] = Double.NaN;
                        logger.info("Cronbach's alpha is set to null because alpha "
                                    + "value is computed to Infinite or NaN.");
                    } else {
                        returnValues[0] = value;
                    }
                }
            }
        }

        //compute alpha while eliminating each column
        for (int i = 0; i < allColumnCnt; i++) {
            if (columnCnt < 3 || rowCnt < 2 || Double.isNaN(varOfColumn[i])) {
                returnValues[i + 1] = Double.NaN;
            } else {
                //recompute sum of item variance
                double subsetSumOfColumnVariance = sumOfColumnVariance - varOfColumn[i];
                //recompute variance of total
                double[] totalWithoutThisCol = new double[allRowCnt];
                for (int j = 0; j < allRowCnt; j++) {
                    if (Double.isNaN(rowAvgMultiByColumnCount[j])) {
                        totalWithoutThisCol[j] = Double.NaN;
                    } else {
                        if (Double.isNaN(data.getEntry(j, i))) {
                            totalWithoutThisCol[j] =
                                (rowAvgMultiByColumnCount[j]/(double)columnCnt)
                                *(double)(columnCnt-1);
                        } else {
                            double sumofThisRow =
                                (rowAvgMultiByColumnCount[j]/(double)columnCnt)*columnCounts[j];
                            totalWithoutThisCol[j] =
                                ((sumofThisRow - data.getEntry(j, i))
                                 /((double)columnCounts[j]-1))*(double)(columnCnt-1);
                        }
                    }
                }

                //compute variance for row total without this item
                //exclude NaN
                ArrayList<Double> rowTotalAL = new ArrayList<Double>();
                for (int j = 0; j < totalWithoutThisCol.length; j++) {
                    if (!Double.isNaN(totalWithoutThisCol[j])) {
                        rowTotalAL.add(totalWithoutThisCol[j]);
                    }
                }
                if (rowTotalAL.size() < 2) {
                    returnValues[i + 1] = Double.NaN;
                } else {
                    double[] rowTotalValues =
                        ArrayUtils.toPrimitive(rowTotalAL.toArray(new Double[0]));
                    double varianceOfTotal = StatUtils.variance(rowTotalValues);
                    double value = computeAlphaValue(columnCnt-1,
                                                     subsetSumOfColumnVariance,
                                                     varianceOfTotal);
                    if (Double.isInfinite(value) || Double.isNaN(value)) {
                        returnValues[i + 1] = Double.NaN;
                        logger.info("Cronbach's alpha is set to null because alpha "
                                    + "value is computed to Infinite or NaN.");
                    } else
                        returnValues[i + 1] = value;
                }
            }
        }

        Double[] returnValues_D = new Double[returnValues.length];
        for (int i = 0; i < returnValues.length; i++) {
            if (Double.isNaN(returnValues[i])) {
                returnValues_D[i] = null;
            } else {
                returnValues_D[i] = returnValues[i];
            }
        }
                
        return returnValues_D;
    }

    private double computeAlphaValue (double count,
                                      double sumOfItemVariance, double varianceOfTotal) {
        return ((double)(count/(count-1)))*(1-sumOfItemVariance/varianceOfTotal);
    }
}
